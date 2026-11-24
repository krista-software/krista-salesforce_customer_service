/*
 * Salesforce Customer Service Extension for Krista
 * Copyright (C) 2025 Krista Software
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package app.krista.extensions.crm.customer_relations.salesforce_customer_service.impl.stores;

import app.krista.extensions.crm.customer_relations.salesforce_customer_service.SalesforceTestBase;
import app.krista.ksdk.files.FileHandle;
import app.krista.ksdk.files.FileRepository;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link KristaMediaClient}.
 *
 * <p>The static {@code compressFile} helper is tested against real temp files. The
 * repository-backed methods ({@code toKristaFile}, {@code toJavaFile}) are tested with a
 * mocked {@link FileRepository}/{@link FileHandle} injected into the {@code @Inject} field
 * via reflection.</p>
 */
public class KristaMediaClientTest extends SalesforceTestBase {

    @Mock
    private FileRepository fileRepository;

    @Mock
    private FileHandle fileHandle;

    private KristaMediaClient client;

    @Before
    public void setUp() throws Exception {
        client = new KristaMediaClient();
        injectRepository(client, fileRepository);
    }

    private static void injectRepository(KristaMediaClient target, FileRepository repo) throws Exception {
        Field field = KristaMediaClient.class.getDeclaredField("fileRepository");
        field.setAccessible(true);
        field.set(target, repo);
    }

    // ==================== compressFile() ====================

    @Test
    public void testCompressFile_CreatesZipWithSourceEntryAndContent() throws Exception {
        File dir = Files.createTempDirectory("kmc-zip").toFile();
        File source = new File(dir, "source.txt");
        Files.write(source.toPath(), "Zip me!".getBytes(StandardCharsets.UTF_8));
        File zip = new File(dir, "out.zip");

        KristaMediaClient.compressFile(zip.getAbsolutePath(), source.getAbsolutePath());

        assertTrue("Zip file should be created", zip.exists());
        assertTrue("Zip file should not be empty", zip.length() > 0);

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zip))) {
            ZipEntry entry = zis.getNextEntry();
            assertNotNull("Zip should contain an entry", entry);
            assertEquals("Entry name should match source file name", "source.txt", entry.getName());

            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            byte[] buffer = new byte[64];
            int read;
            while ((read = zis.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            String content = out.toString(StandardCharsets.UTF_8);
            assertEquals("Entry content should match source", "Zip me!", content);
            assertNull("Only one entry expected", zis.getNextEntry());
        } finally {
            source.delete();
            zip.delete();
            dir.delete();
        }
    }

    // ==================== toKristaFile() ====================

    @Test
    public void testToKristaFile_WithSupportedFormat_UploadsOriginalFile() throws Exception {
        File dir = Files.createTempDirectory("kmc-supported").toFile();
        File source = new File(dir, "test.txt");
        Files.write(source.toPath(), "hello".getBytes(StandardCharsets.UTF_8));

        app.krista.model.base.File expected =
                new app.krista.model.base.File(5L, "test.txt", "txt", "media-1");
        when(fileRepository.createNewFileByName("test.txt")).thenReturn(fileHandle);
        when(fileHandle.getFile()).thenReturn(expected);

        try {
            app.krista.model.base.File result = client.toKristaFile(source);

            assertSame("Should return the FileHandle's file", expected, result);
            verify(fileRepository).createNewFileByName("test.txt");
            verify(fileHandle).setContent(any());
            verify(fileHandle).getFile();
        } finally {
            source.delete();
            dir.delete();
        }
    }

    @Test
    public void testToKristaFile_WithUnsupportedFormat_ZipsThenUploadsAndCleansUp() throws Exception {
        File dir = Files.createTempDirectory("kmc-unsupported").toFile();
        File source = new File(dir, "test.js");
        Files.write(source.toPath(), "alert('x');".getBytes(StandardCharsets.UTF_8));

        // Zip path is derived as <java.io.tmpdir>/<base-name>.zip
        String tmpDir = System.getProperty("java.io.tmpdir");
        File expectedZip = new File(tmpDir + File.separator + "test.zip");

        app.krista.model.base.File expected =
                new app.krista.model.base.File(1L, "test.zip", "zip", "media-2");
        when(fileRepository.createNewFileByName("test.zip")).thenReturn(fileHandle);
        when(fileHandle.getFile()).thenReturn(expected);

        try {
            app.krista.model.base.File result = client.toKristaFile(source);

            assertSame("Should return the FileHandle's file", expected, result);
            verify(fileRepository).createNewFileByName("test.zip");
            verify(fileHandle).setContent(any());
            assertFalse("Temporary zip file should be deleted in finally block", expectedZip.exists());
        } finally {
            source.delete();
            dir.delete();
            if (expectedZip.exists()) {
                expectedZip.delete();
            }
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testToKristaFile_WithNoExtension_ThrowsIllegalArgument() throws Exception {
        // getFileExtension throws for a name without a dot; happens before any file IO.
        client.toKristaFile(new File("filewithoutextension"));
    }

    // ==================== toJavaFile() ====================

    @Test
    public void testToJavaFile_WritesRepositoryContentToLocalFile() throws Exception {
        File dir = Files.createTempDirectory("kmc-download").toFile();
        String outPath = new File(dir, "download.dat").getAbsolutePath();
        app.krista.model.base.File krFile =
                new app.krista.model.base.File(5L, outPath, "dat", "media-3");

        when(fileRepository.getFile(any())).thenReturn(fileHandle);
        when(fileHandle.getContent())
                .thenReturn(new ByteArrayInputStream("hello".getBytes(StandardCharsets.UTF_8)));

        File result = null;
        try {
            result = client.toJavaFile(krFile);

            assertNotNull(result);
            assertTrue("Downloaded file should exist", result.exists());
            String content = new String(Files.readAllBytes(result.toPath()), StandardCharsets.UTF_8);
            assertEquals("Content should be copied from the repository stream", "hello", content);
            verify(fileRepository).getFile(any());
            verify(fileHandle).getContent();
        } finally {
            if (result != null) {
                result.delete();
            }
            dir.delete();
        }
    }
}
