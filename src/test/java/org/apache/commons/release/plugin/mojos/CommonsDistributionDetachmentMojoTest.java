/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.release.plugin.mojos;

import org.apache.maven.plugin.testing.MojoRule;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

/**
 * Unit tests for {@link CommonsDistributionDetachmentMojo}.
 *
 * @author chtompki
 */
public class CommonsDistributionDetachmentMojoTest {

    @Rule
    public MojoRule rule = new MojoRule() {
        @Override
        protected void before() throws Throwable {
        }

        @Override
        protected void after() {
        }
    };

    private CommonsDistributionDetachmentMojo mojo;

    @Test
    public void testSuccess() throws Exception {
        File testPom = new File("src/test/resources/mojos/detach-distributions/detach-distributions.xml");
        assertNotNull(testPom);
        assertTrue(testPom.exists());
        mojo = (CommonsDistributionDetachmentMojo) rule.lookupMojo("detach-distributions", testPom);
        mojo.execute();
        File detachedTarGz = new File("target/commons-release-plugin/mockAttachedTar.tar.gz");
        File detachedTarGzAsc = new File("target/commons-release-plugin/mockAttachedTar.tar.gz.asc");
        File detachedTarMd5 = new File("target/commons-release-plugin/mockAttachedTar.tar.gz.md5");
        File detachedTarGzSha1 = new File("target/commons-release-plugin/mockAttachedTar.tar.gz.sha1");
        File detachedZip = new File("target/commons-release-plugin/mockAttachedZip.zip");
        File detachedZipAsc = new File("target/commons-release-plugin/mockAttachedZip.zip.asc");
        File detachedZipMd5 = new File("target/commons-release-plugin/mockAttachedZip.zip.md5");
        File detachedZipSha1 = new File("target/commons-release-plugin/mockAttachedZip.zip.sha1");
        File notDetatchedMockAttachedFile = new File("target/commons-release-plugin/mockAttachedFile.html");
        assertTrue(detachedTarGz.exists());
        assertTrue(detachedTarGzAsc.exists());
        assertTrue(detachedTarMd5.exists());
        assertTrue(detachedTarGzSha1.exists());
        assertTrue(detachedZip.exists());
        assertTrue(detachedZipAsc.exists());
        assertTrue(detachedZipMd5.exists());
        assertTrue(detachedZipSha1.exists());
        assertFalse(notDetatchedMockAttachedFile.exists());
    }
}