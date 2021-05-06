/*
 * Tencent is pleased to support the open source community by making Tinker available.
 *
 * Copyright (C) 2016 THL A29 Limited, a Tencent company. All rights reserved.
 *
 * Licensed under the BSD 3-Clause License (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * https://opensource.org/licenses/BSD-3-Clause
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.orange.tinkerhotfix.patch.decode;



import com.orange.tinkerhotfix.patch.Configuration;
import com.orange.tinkerhotfix.patch.TinkerPatchException;
import com.orange.tinkerhotfix.patch.util.FileOperation;
import com.orange.tinkerhotfix.patch.util.Logger;
import com.orange.tinkerhotfix.patch.util.TypedValue;
import com.orange.tinkerhotfix.patch.util.Utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;

/**
 * Created by zhangshaowen on 16/3/15.
 */
public class ApkDecoder extends BaseDecoder {
    private final File mOldApkDir;
    private final File mNewApkDir;

    private final UniqueDexDiffDecoder dexPatchDecoder;

    /**
     * if resource's file is also contain in dex or library pattern,
     * they won't change in new resources' apk, and we will just warn you.
     */
    ArrayList<File> resDuplicateFiles;

    public ApkDecoder(Configuration config) throws IOException {
        super(config);
        this.mNewApkDir = config.mTempUnzipNewDir;
        this.mOldApkDir = config.mTempUnzipOldDir;

        //put meta files in assets
        String prePath = TypedValue.FILE_ASSETS + File.separator;
        dexPatchDecoder = new UniqueDexDiffDecoder(config, prePath + TypedValue.DEX_META_FILE, TypedValue.DEX_LOG_FILE);
    }

    private void unzipApkFile(File file, File destFile) throws TinkerPatchException, IOException {
        String apkName = file.getName();
        if (!apkName.endsWith(TypedValue.FILE_APK)) {
            throw new TinkerPatchException(
                String.format("input apk file path must end with .apk, yours %s\n", apkName)
            );
        }

        String destPath = destFile.getAbsolutePath();
        Logger.d("UnZipping apk to %s", destPath);
        FileOperation.unZipAPk(file.getAbsoluteFile().getAbsolutePath(), destPath);

    }

    private void unzipApkFiles(File oldFile, File newFile) throws IOException, TinkerPatchException {
        unzipApkFile(oldFile, this.mOldApkDir);
        unzipApkFile(newFile, this.mNewApkDir);
    }

    @Override
    public void onAllPatchesStart() throws IOException, TinkerPatchException {
        dexPatchDecoder.onAllPatchesStart();
    }

    public boolean patch(File oldFile, File newFile) throws Exception {
//        writeToLogFile(oldFile, newFile);
        //check manifest change first
//        manifestDecoder.patch(oldFile, newFile);

        unzipApkFiles(oldFile, newFile);

        Files.walkFileTree(mNewApkDir.toPath(), new ApkFilesVisitor(mNewApkDir.toPath(), mOldApkDir.toPath(), dexPatchDecoder));

        // get all duplicate resource file
//        for (File duplicateRes : resDuplicateFiles) {
//            // resPatchDecoder.patch(duplicateRes, null);
//            Logger.e("Warning: res file %s is also match at dex or library pattern, "
//                + "we treat it as unchanged in the new resource_out.zip", getRelativePathStringToOldFile(duplicateRes));
//        }

        dexPatchDecoder.onAllPatchesEnd();

        //clean resources
        dexPatchDecoder.clean();

        return true;
    }

    @Override
    public void onAllPatchesEnd() throws IOException, TinkerPatchException {
    }

    class ApkFilesVisitor extends SimpleFileVisitor<Path> {
        BaseDecoder     dexDecoder;
        Path            newApkPath;
        Path            oldApkPath;

        ApkFilesVisitor(Path newPath, Path oldPath, BaseDecoder dex) {
            this.dexDecoder = dex;
            this.newApkPath = newPath;
            this.oldApkPath = oldPath;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
//            System.out.println("[ApkDecoder] file = " + file.toString());
//            if(!file.toString().endsWith(".dex")){
//                return FileVisitResult.CONTINUE;
//            }
//            Path relativePath = newApkPath.relativize(file);
//            System.out.println("[ApkDecoder] relativePath = " + relativePath.toString());
//            Path oldPath = oldApkPath.resolve(relativePath);
//            System.out.println("[ApkDecoder] oldPath = " + oldPath.toString());
//            File oldFile = null;
//            //is a new file?!
//            if (oldPath.toFile().exists()) {
//                oldFile = oldPath.toFile();
//                System.out.println("[ApkDecoder] oldFile = " + oldFile.getName());
//            }
//            try {
//                dexDecoder.patch(oldFile, file.toFile());
//            } catch (Exception e) {
//                throw new RuntimeException(e);
//            }
//
//            return FileVisitResult.CONTINUE;
            Path relativePath = newApkPath.relativize(file);

            Path oldPath = oldApkPath.resolve(relativePath);

            File oldFile = null;
            //is a new file?!
            if (oldPath.toFile().exists()) {
                oldFile = oldPath.toFile();
            }
            String patternKey = relativePath.toString().replace("\\", "/");

            if (Utils.checkFileInPattern(config.mDexFilePattern, patternKey)) {
                //also treat duplicate file as unchanged
                try {
                    dexDecoder.patch(oldFile, file.toFile());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                return FileVisitResult.CONTINUE;
            }

            return FileVisitResult.CONTINUE;
        }

    }
}
