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

package com.orange.tinkerhotfix.dexpatcher.algorithms.diff;


import com.orange.tinkerhotfix.common.AbstractIndexMap;
import com.orange.tinkerhotfix.common.SparseIndexMap;
import com.orange.tinkerhotfix.party.AnnotationSet;
import com.orange.tinkerhotfix.party.Dex;
import com.orange.tinkerhotfix.party.TableOfContents;
import com.orange.tinkerhotfix.party.io.DexDataBuffer;
import com.orange.tinkerhotfix.struct.DexPatchFile;

/**
 * Created by tangyinsheng on 2016/7/4.
 */
public class AnnotationSetSectionPatchAlgorithm extends DexSectionPatchAlgorithm<AnnotationSet> {
    private TableOfContents.Section patchedAnnotationSetTocSec = null;
    private Dex.Section patchedAnnotationSetSec = null;

    public AnnotationSetSectionPatchAlgorithm(
            DexPatchFile patchFile,
            Dex oldDex,
            Dex patchedDex,
            SparseIndexMap oldToPatchedIndexMap
    ) {
        super(patchFile, oldDex, oldToPatchedIndexMap);
        if (patchedDex != null) {
            this.patchedAnnotationSetTocSec = patchedDex.getTableOfContents().annotationSets;
            this.patchedAnnotationSetSec = patchedDex.openSection(this.patchedAnnotationSetTocSec);
        }
    }

    @Override
    protected TableOfContents.Section getTocSection(Dex dex) {
        return dex.getTableOfContents().annotationSets;
    }

    @Override
    protected AnnotationSet nextItem(DexDataBuffer section) {
        return section.readAnnotationSet();
    }

    @Override
    protected int getItemSize(AnnotationSet item) {
        return item.byteCountInDex();
    }

    @Override
    protected AnnotationSet adjustItem(AbstractIndexMap indexMap, AnnotationSet item) {
        return indexMap.adjust(item);
    }

    @Override
    protected int writePatchedItem(AnnotationSet patchedItem) {
        ++this.patchedAnnotationSetTocSec.size;
        return this.patchedAnnotationSetSec.writeAnnotationSet(patchedItem);
    }

    @Override
    protected void updateIndexOrOffset(SparseIndexMap sparseIndexMap, int oldIndex, int oldOffset, int newIndex, int newOffset) {
        if (oldOffset != newOffset) {
            sparseIndexMap.mapAnnotationSetOffset(oldOffset, newOffset);
        }
    }

    @Override
    protected void markDeletedIndexOrOffset(SparseIndexMap sparseIndexMap, int deletedIndex, int deletedOffset) {
        sparseIndexMap.markAnnotationSetDeleted(deletedOffset);
    }
}
