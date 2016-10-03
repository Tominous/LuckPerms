/*
 * Copyright (c) 2016 Lucko (Luck) <luck@lucko.me>
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

package me.lucko.luckperms.calculators;

import lombok.AllArgsConstructor;
import lombok.Getter;
import me.lucko.luckperms.api.Tristate;

import java.util.Map;

@AllArgsConstructor
public class WildcardProcessor implements PermissionProcessor {

    @Getter
    private final Map<String, Boolean> map;

    @Override
    public Tristate hasPermission(String permission) {
        String node = permission;

        while (node.contains(".")) {
            int endIndex = node.lastIndexOf('.');
            if (endIndex == -1) {
                break;
            }

            node = node.substring(0, endIndex);
            if (!isEmpty(node)) {
                if (map.containsKey(node + ".*")) {
                    return Tristate.fromBoolean(map.get(node + ".*"));
                }
            }
        }

        if (map.containsKey("'*'")) {
            return Tristate.fromBoolean(map.get("'*'"));
        }

        if (map.containsKey("*")) {
            return Tristate.fromBoolean(map.get("*"));
        }

        return Tristate.UNDEFINED;
    }

    private static boolean isEmpty(String s) {
        if (s.equals("")) {
            return true;
        }

        char[] chars = s.toCharArray();
        for (char c : chars) {
            if (c != '.') {
                return false;
            }
        }

        return true;
    }
}
