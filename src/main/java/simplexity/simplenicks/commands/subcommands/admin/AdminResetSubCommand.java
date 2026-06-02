package simplexity.simplenicks.commands.subcommands.admin;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import simplexity.simplenicks.util.FoliaScheduler;
import org.jetbrains.annotations.NotNull;
import simplexity.simplenicks.SimpleNicks;
import simplexity.simplenicks.commands.NicknameProcessor;
import simplexity.simplenicks.commands.arguments.OfflinePlayerArgument;
import simplexity.simplenicks.commands.subcommands.basic.SubCommand;
import simplexity.simplenicks.config.LocaleMessage;
import simplexity.simplenicks.logic.NickUtils;
import simplexity.simplenicks.util.NickPermission;

@SuppressWarnings("UnstableApiUsage")
public class AdminResetSubCommand implements SubCommand {
    @Override
    public void subcommandTo(@NotNull LiteralArgumentBuilder<CommandSourceStack> parent) {

        OfflinePlayerArgument offlinePlayerArgument = new OfflinePlayerArgument();

        parent.then(Commands.literal("reset")
                .requires(this::canExecute)
                .then(Commands.argument("player", offlinePlayerArgument)
                        .suggests(offlinePlayerArgument::suggestOnlinePlayers)
                        .executes(this::execute)));

    }

    @Override
    public int execute(@NotNull CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CommandSender sender = ctx.getSource().getSender();
        OfflinePlayer target = ctx.getArgument("player", OfflinePlayer.class);
        FoliaScheduler.async(SimpleNicks.getInstance(), () -> {
            boolean success = NicknameProcessor.getInstance().resetNickname(target);
            if (!success) {
                sender.sendRichMessage(LocaleMessage.ERROR_RESET_FAILURE.getMessage());
                return;
            }
            if (target instanceof Player onlineTarget) {
                FoliaScheduler.onEntity(SimpleNicks.getInstance(), onlineTarget, () -> {
                    NickUtils.refreshDisplayName(target.getUniqueId());
                    onlineTarget.sendMessage(parseAdminMessage(LocaleMessage.RESET_BY_INITIATOR.getMessage(), "", sender, target));
                });
            }
            sender.sendMessage(parseAdminMessage(LocaleMessage.RESET_TARGET.getMessage(), "", sender, target));
        });
        return Command.SINGLE_SUCCESS;
    }

    @Override
    public boolean canExecute(@NotNull CommandSourceStack css) {
        CommandSender sender = css.getSender();
        return sender.hasPermission(NickPermission.NICK_ADMIN_RESET.getPermission());
    }

}
