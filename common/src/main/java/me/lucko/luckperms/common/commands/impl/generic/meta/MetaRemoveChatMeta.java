/*
 * This file is part of LuckPerms, licensed under the MIT License.
 *
 *  Copyright (c) lucko (Luck) <luck@lucko.me>
 *  Copyright (c) contributors
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package me.lucko.luckperms.common.commands.impl.generic.meta;

import me.lucko.luckperms.api.ChatMetaType;
import me.lucko.luckperms.api.DataMutateResult;
import me.lucko.luckperms.api.context.MutableContextSet;
import me.lucko.luckperms.common.commands.Arg;
import me.lucko.luckperms.common.commands.CommandException;
import me.lucko.luckperms.common.commands.CommandResult;
import me.lucko.luckperms.common.commands.abstraction.SharedSubCommand;
import me.lucko.luckperms.common.commands.sender.Sender;
import me.lucko.luckperms.common.commands.utils.ArgumentUtils;
import me.lucko.luckperms.common.commands.utils.Util;
import me.lucko.luckperms.common.constants.Message;
import me.lucko.luckperms.common.constants.Permission;
import me.lucko.luckperms.common.core.NodeFactory;
import me.lucko.luckperms.common.core.model.PermissionHolder;
import me.lucko.luckperms.common.data.LogEntry;
import me.lucko.luckperms.common.plugin.LuckPermsPlugin;
import me.lucko.luckperms.common.utils.Predicates;

import java.util.List;
import java.util.stream.Collectors;

public class MetaRemoveChatMeta extends SharedSubCommand {
    private final ChatMetaType type;

    public MetaRemoveChatMeta(ChatMetaType type) {
        super("remove" + type.name().toLowerCase(),
                "Removes a " + type.name().toLowerCase(),
                type == ChatMetaType.PREFIX ? Permission.USER_META_REMOVEPREFIX : Permission.USER_META_REMOVESUFFIX,
                type == ChatMetaType.PREFIX ? Permission.GROUP_META_REMOVEPREFIX : Permission.GROUP_META_REMOVESUFFIX,
                Predicates.is(0),
                Arg.list(
                        Arg.create("priority", true, "the priority to remove the " + type.name().toLowerCase() + " at"),
                        Arg.create(type.name().toLowerCase(), false, "the " + type.name().toLowerCase() + " string"),
                        Arg.create("context...", false, "the contexts to remove the " + type.name().toLowerCase() + " in")
                )
        );
        this.type = type;
    }

    @Override
    public CommandResult execute(LuckPermsPlugin plugin, Sender sender, PermissionHolder holder, List<String> args, String label) throws CommandException {
        int priority = ArgumentUtils.handlePriority(0, args);
        String meta = ArgumentUtils.handleStringOrElse(1, args, "null");
        MutableContextSet context = ArgumentUtils.handleContext(2, args, plugin);

        // Handle bulk removal
        if (meta.equalsIgnoreCase("null") || meta.equals("*")) {
            holder.removeIf(n ->
                    type.matches(n) &&
                    type.getEntry(n).getKey() == priority &&
                    !n.isTemporary() &&
                    n.getFullContexts().makeImmutable().equals(context.makeImmutable())
            );
            Message.BULK_REMOVE_CHATMETA_SUCCESS.send(sender, holder.getFriendlyName(), type.name().toLowerCase(), priority, Util.contextSetToString(context));
            save(holder, sender, plugin);
            return CommandResult.SUCCESS;
        }

        DataMutateResult result = holder.unsetPermission(NodeFactory.makeChatMetaNode(type, priority, meta).withExtraContext(context).build());

        if (result.asBoolean()) {
            Message.REMOVE_CHATMETA_SUCCESS.send(sender, holder.getFriendlyName(), type.name().toLowerCase(), meta, priority, Util.contextSetToString(context));

            LogEntry.build().actor(sender).acted(holder)
                    .action("meta remove" + type.name().toLowerCase() + " " + args.stream().map(ArgumentUtils.WRAPPER).collect(Collectors.joining(" ")))
                    .build().submit(plugin, sender);

            save(holder, sender, plugin);
            return CommandResult.SUCCESS;
        } else {
            Message.DOES_NOT_HAVE_CHAT_META.send(sender, holder.getFriendlyName(), type.name().toLowerCase());
            return CommandResult.STATE_ERROR;
        }
    }
}