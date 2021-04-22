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

package com.orange.tinkerhotfix.common;


import com.orange.tinkerhotfix.dexpatcher.algorithms.diff.AnnotationSectionPatchAlgorithm;
import com.orange.tinkerhotfix.dexpatcher.algorithms.diff.AnnotationSetRefListSectionPatchAlgorithm;
import com.orange.tinkerhotfix.dexpatcher.algorithms.diff.AnnotationSetSectionPatchAlgorithm;
import com.orange.tinkerhotfix.dexpatcher.algorithms.diff.AnnotationsDirectorySectionPatchAlgorithm;
import com.orange.tinkerhotfix.dexpatcher.algorithms.diff.ClassDataSectionPatchAlgorithm;
import com.orange.tinkerhotfix.dexpatcher.algorithms.diff.ClassDefSectionPatchAlgorithm;
import com.orange.tinkerhotfix.dexpatcher.algorithms.diff.CodeSectionPatchAlgorithm;
import com.orange.tinkerhotfix.dexpatcher.algorithms.diff.DebugInfoItemSectionPatchAlgorithm;
import com.orange.tinkerhotfix.dexpatcher.algorithms.diff.DexSectionPatchAlgorithm;
import com.orange.tinkerhotfix.dexpatcher.algorithms.diff.FieldIdSectionPatchAlgorithm;
import com.orange.tinkerhotfix.dexpatcher.algorithms.diff.MethodIdSectionPatchAlgorithm;
import com.orange.tinkerhotfix.dexpatcher.algorithms.diff.ProtoIdSectionPatchAlgorithm;
import com.orange.tinkerhotfix.dexpatcher.algorithms.diff.StaticValueSectionPatchAlgorithm;
import com.orange.tinkerhotfix.dexpatcher.algorithms.diff.StringDataSectionPatchAlgorithm;
import com.orange.tinkerhotfix.dexpatcher.algorithms.diff.TypeIdSectionPatchAlgorithm;
import com.orange.tinkerhotfix.dexpatcher.algorithms.diff.TypeListSectionPatchAlgorithm;
import com.orange.tinkerhotfix.party.Annotation;
import com.orange.tinkerhotfix.party.AnnotationSet;
import com.orange.tinkerhotfix.party.AnnotationSetRefList;
import com.orange.tinkerhotfix.party.AnnotationsDirectory;
import com.orange.tinkerhotfix.party.ClassData;
import com.orange.tinkerhotfix.party.ClassDef;
import com.orange.tinkerhotfix.party.Code;
import com.orange.tinkerhotfix.party.DebugInfoItem;
import com.orange.tinkerhotfix.party.Dex;
import com.orange.tinkerhotfix.party.EncodedValue;
import com.orange.tinkerhotfix.party.FieldId;
import com.orange.tinkerhotfix.party.MethodId;
import com.orange.tinkerhotfix.party.ProtoId;
import com.orange.tinkerhotfix.party.StringData;
import com.orange.tinkerhotfix.party.TableOfContents;
import com.orange.tinkerhotfix.party.TypeList;
import com.orange.tinkerhotfix.party.util.CompareUtils;
import com.orange.tinkerhotfix.patch.util.IOHelper;
import com.orange.tinkerhotfix.struct.DexPatchFile;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

/**
 * Created by tangyinsheng on 2016/6/30.
 */
public class DexPatchApplier {
    private final Dex oldDex;
    private final Dex patchedDex;

    private final DexPatchFile patchFile;

    private final SparseIndexMap oldToPatchedIndexMap;

    private DexSectionPatchAlgorithm<StringData> stringDataSectionPatchAlg;
    private DexSectionPatchAlgorithm<Integer> typeIdSectionPatchAlg;
    private DexSectionPatchAlgorithm<ProtoId> protoIdSectionPatchAlg;
    private DexSectionPatchAlgorithm<FieldId> fieldIdSectionPatchAlg;
    private DexSectionPatchAlgorithm<MethodId> methodIdSectionPatchAlg;
    private DexSectionPatchAlgorithm<ClassDef> classDefSectionPatchAlg;
    private DexSectionPatchAlgorithm<TypeList> typeListSectionPatchAlg;
    private DexSectionPatchAlgorithm<AnnotationSetRefList> annotationSetRefListSectionPatchAlg;
    private DexSectionPatchAlgorithm<AnnotationSet> annotationSetSectionPatchAlg;
    private DexSectionPatchAlgorithm<ClassData> classDataSectionPatchAlg;
    private DexSectionPatchAlgorithm<Code> codeSectionPatchAlg;
    private DexSectionPatchAlgorithm<DebugInfoItem> debugInfoSectionPatchAlg;
    private DexSectionPatchAlgorithm<Annotation> annotationSectionPatchAlg;
    private DexSectionPatchAlgorithm<EncodedValue> encodedArraySectionPatchAlg;
    private DexSectionPatchAlgorithm<AnnotationsDirectory> annotationsDirectorySectionPatchAlg;

    public DexPatchApplier(File oldDexIn, File patchFileIn) throws IOException {
        this(new Dex(oldDexIn), new DexPatchFile(patchFileIn));
    }

    public DexPatchApplier(InputStream oldDexIn, InputStream patchFileIn) throws IOException {
        this(new Dex(oldDexIn), new DexPatchFile(patchFileIn));
    }

    public DexPatchApplier(
            Dex oldDexIn,
            DexPatchFile patchFileIn
    ) {
        this.oldDex = oldDexIn;
        this.patchFile = patchFileIn;
        this.patchedDex = new Dex(patchFileIn.getPatchedDexSize());
        this.oldToPatchedIndexMap = new SparseIndexMap();
    }

    public void executeAndSaveTo(OutputStream out) throws IOException {
        // Before executing, we should check if this patch can be applied to
        // old dex we passed in.
        byte[] oldDexSign = this.oldDex.computeSignature(false);
        if (oldDexSign == null) {
            throw new IOException("failed to compute old dex's signature.");
        }
        if (this.patchFile == null) {
            throw new IllegalArgumentException("patch file is null.");
        }
        byte[] oldDexSignInPatchFile = this.patchFile.getOldDexSignature();
        if (CompareUtils.uArrCompare(oldDexSign, oldDexSignInPatchFile) != 0) {
            throw new IOException(
                    String.format(
                            "old dex signature mismatch! expected: %s, actual: %s",
                            Arrays.toString(oldDexSign),
                            Arrays.toString(oldDexSignInPatchFile)
                    )
            );
        }

        // Firstly, set sections' offset after patched, sort according to their offset so that
        // the dex lib of aosp can calculate section size.
        TableOfContents patchedToc = this.patchedDex.getTableOfContents();

        patchedToc.header.off = 0;
        patchedToc.header.size = 1;
        patchedToc.mapList.size = 1;

        patchedToc.stringIds.off
                = this.patchFile.getPatchedStringIdSectionOffset();
        patchedToc.typeIds.off
                = this.patchFile.getPatchedTypeIdSectionOffset();
        patchedToc.typeLists.off
                = this.patchFile.getPatchedTypeListSectionOffset();
        patchedToc.protoIds.off
                = this.patchFile.getPatchedProtoIdSectionOffset();
        patchedToc.fieldIds.off
                = this.patchFile.getPatchedFieldIdSectionOffset();
        patchedToc.methodIds.off
                = this.patchFile.getPatchedMethodIdSectionOffset();
        patchedToc.classDefs.off
                = this.patchFile.getPatchedClassDefSectionOffset();
        patchedToc.mapList.off
                = this.patchFile.getPatchedMapListSectionOffset();
        patchedToc.stringDatas.off
                = this.patchFile.getPatchedStringDataSectionOffset();
        patchedToc.annotations.off
                = this.patchFile.getPatchedAnnotationSectionOffset();
        patchedToc.annotationSets.off
                = this.patchFile.getPatchedAnnotationSetSectionOffset();
        patchedToc.annotationSetRefLists.off
                = this.patchFile.getPatchedAnnotationSetRefListSectionOffset();
        patchedToc.annotationsDirectories.off
                = this.patchFile.getPatchedAnnotationsDirectorySectionOffset();
        patchedToc.encodedArrays.off
                = this.patchFile.getPatchedEncodedArraySectionOffset();
        patchedToc.debugInfos.off
                = this.patchFile.getPatchedDebugInfoSectionOffset();
        patchedToc.codes.off
                = this.patchFile.getPatchedCodeSectionOffset();
        patchedToc.classDatas.off
                = this.patchFile.getPatchedClassDataSectionOffset();
        patchedToc.fileSize
                = this.patchFile.getPatchedDexSize();

        Arrays.sort(patchedToc.sections);

        patchedToc.computeSizesFromOffsets();

        // Secondly, run patch algorithms according to sections' dependencies.
        this.stringDataSectionPatchAlg = new StringDataSectionPatchAlgorithm(
                patchFile, oldDex, patchedDex, oldToPatchedIndexMap
        );
        this.typeIdSectionPatchAlg = new TypeIdSectionPatchAlgorithm(
                patchFile, oldDex, patchedDex, oldToPatchedIndexMap
        );
        this.protoIdSectionPatchAlg = new ProtoIdSectionPatchAlgorithm(
                patchFile, oldDex, patchedDex, oldToPatchedIndexMap
        );
        this.fieldIdSectionPatchAlg = new FieldIdSectionPatchAlgorithm(
                patchFile, oldDex, patchedDex, oldToPatchedIndexMap
        );
        this.methodIdSectionPatchAlg = new MethodIdSectionPatchAlgorithm(
                patchFile, oldDex, patchedDex, oldToPatchedIndexMap
        );
        this.classDefSectionPatchAlg = new ClassDefSectionPatchAlgorithm(
                patchFile, oldDex, patchedDex, oldToPatchedIndexMap
        );
        this.typeListSectionPatchAlg = new TypeListSectionPatchAlgorithm(
                patchFile, oldDex, patchedDex, oldToPatchedIndexMap
        );
        this.annotationSetRefListSectionPatchAlg = new AnnotationSetRefListSectionPatchAlgorithm(
                patchFile, oldDex, patchedDex, oldToPatchedIndexMap
        );
        this.annotationSetSectionPatchAlg = new AnnotationSetSectionPatchAlgorithm(
                patchFile, oldDex, patchedDex, oldToPatchedIndexMap
        );
        this.classDataSectionPatchAlg = new ClassDataSectionPatchAlgorithm(
                patchFile, oldDex, patchedDex, oldToPatchedIndexMap
        );
        this.codeSectionPatchAlg = new CodeSectionPatchAlgorithm(
                patchFile, oldDex, patchedDex, oldToPatchedIndexMap
        );
        this.debugInfoSectionPatchAlg = new DebugInfoItemSectionPatchAlgorithm(
                patchFile, oldDex, patchedDex, oldToPatchedIndexMap
        );
        this.annotationSectionPatchAlg = new AnnotationSectionPatchAlgorithm(
                patchFile, oldDex, patchedDex, oldToPatchedIndexMap
        );
        this.encodedArraySectionPatchAlg = new StaticValueSectionPatchAlgorithm(
                patchFile, oldDex, patchedDex, oldToPatchedIndexMap
        );
        this.annotationsDirectorySectionPatchAlg = new AnnotationsDirectorySectionPatchAlgorithm(
                patchFile, oldDex, patchedDex, oldToPatchedIndexMap
        );

        this.stringDataSectionPatchAlg.execute();
        this.typeIdSectionPatchAlg.execute();
        this.typeListSectionPatchAlg.execute();
        this.protoIdSectionPatchAlg.execute();
        this.fieldIdSectionPatchAlg.execute();
        this.methodIdSectionPatchAlg.execute();
        this.annotationSectionPatchAlg.execute();
        this.annotationSetSectionPatchAlg.execute();
        this.annotationSetRefListSectionPatchAlg.execute();
        this.annotationsDirectorySectionPatchAlg.execute();
        this.debugInfoSectionPatchAlg.execute();
        this.codeSectionPatchAlg.execute();
        this.classDataSectionPatchAlg.execute();
        this.encodedArraySectionPatchAlg.execute();
        this.classDefSectionPatchAlg.execute();

        // Thirdly, write header, mapList. Calculate and write patched dex's sign and checksum.
        Dex.Section headerOut = this.patchedDex.openSection(patchedToc.header.off);
        patchedToc.writeHeader(headerOut);

        Dex.Section mapListOut = this.patchedDex.openSection(patchedToc.mapList.off);
        patchedToc.writeMap(mapListOut);

        this.patchedDex.writeHashes();

        // Finally, write patched dex to file.
        this.patchedDex.writeTo(out);
    }

    public void executeAndSaveTo(File file) throws IOException {
        OutputStream os = null;
        try {
            os = new BufferedOutputStream(new FileOutputStream(file));
            executeAndSaveTo(os);
        } finally {
            IOHelper.closeQuietly(os);
        }
    }
}
