// spotless:off

/*
 * Copyright 2026 aoqia
 *
 * This file was originally released under LICENSE_UPSTREAM (see below for the original header) and has been relicensed
 * to GNU's AGPLv3 license, with additional terms in the LICENSE_EXCEPTION.md file.
 *
 * All modifications made to this file after the original version were created separate to the original copyright.
 * These modifications are provided under the AGPLv3 license (including exception terms). The portions of this file that
 * were not modified remain subject to the original copyright notice and license, and the original LICENSE is kept in
 * LICENSE_UPSTREAM located alongside LICENSE.
 *
 * The complete record of changes to this file is publicly available in the project's Git history, which anyone may view
 * at any time for any reason.
 *
 * Copyright (c) 2016-2025 FabricMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// spotless:on

package dev.aoqia.leaf.installer.test;

import org.junit.Assert;
import org.junit.Test;

import dev.aoqia.leaf.installer.util.ArgumentParser;

public class ArgumentTests {
    @Test
    public void test() {
        String[] args = new String[] { "command", "-debug", "-cachedir=C:\\SomePath\\SomeFolder", "-novoip" };
        ArgumentParser handler = ArgumentParser.create(args);

        Assert.assertTrue(handler.has("debug"));
        Assert.assertEquals("C:\\SomePath\\SomeFolder", handler.get("cachedir"));

        Assert.assertEquals("World", handler.getOrDefault("arg3", () -> "World"));

        Assert.assertTrue(handler.has("novoip"));
        Assert.assertFalse(handler.has("aitest"));

        Assert.assertEquals("command", handler.getCommand().get());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBadArgs() {
        ArgumentParser.create(new String[] { "-arg1=Hello", "-arg1" });
    }

    @Test
    public void testCommands() {
        Assert.assertTrue(ArgumentParser.create(new String[] { "command", "-arg1=Hello" }).getCommand().isPresent());
        Assert.assertEquals(ArgumentParser.create(new String[] { "command", "-arg1=Hello" }).getCommand().get(),
            "command");

        Assert.assertFalse(ArgumentParser.create(new String[] { "-arg1=Hello" }).getCommand().isPresent());
    }

    @Test
    public void testNullArg() {
        Assert.assertNull(ArgumentParser.create(new String[] { "-arg1" }).get("arg1"));
    }

    @Test
    public void testUnknownArg() {
        Assert.assertNull(ArgumentParser.create(new String[] { "-arg1=Hello" }).get("arg2"));
    }
}
