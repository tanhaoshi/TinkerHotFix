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
import com.orange.tinkerhotfix.party.Dex;
import com.orange.tinkerhotfix.party.MethodId;
import com.orange.tinkerhotfix.party.TableOfContents;
import com.orange.tinkerhotfix.party.io.DexDataBuffer;
import com.orange.tinkerhotfix.struct.DexPatchFile;

/**
 * Created by tangyinsheng on 2016/7/4.
 */
public class MethodIdSectionPatchAlgorithm extends DexSectionPatchAlgorithm<MethodId> {
    private TableOfContents.Section patchedMethodIdTocSec = null;
    private Dex.Section patchedMethodIdSec = null;

    public MethodIdSectionPatchAlgorithm(
            DexPatchFile patchFile,
            Dex oldDex,
            Dex patchedDex,
            SparseIndexMap oldToPatchedIndexMap
    ) {
        super(patchFile, oldDex, oldToPatchedIndexMap);

        if (patchedDex != null) {
            this.patchedMethodIdTocSec = patchedDex.getTableOfContents().methodIds;
            this.patchedMethodIdSec = patchedDex.openSection(this.patchedMethodIdTocSec);
        }
    }

    @Override
    protected TableOfContents.Section getTocSection(Dex dex) {
        return dex.getTableOfContents().methodIds;
    }

    @Override
    protected MethodId nextItem(DexDataBuffer section) {
        return section.readMethodId();
    }

    @Override
    protected int getItemSize(MethodId item) {
        return item.byteCountInDex();
    }

    @Override
    protected MethodId adjustItem(AbstractIndexMap indexMap, MethodId item) {
        return indexMap.adjust(item);
    }

    @Override
    protected int writePatchedItem(MethodId patchedItem) {
        ++this.patchedMethodIdTocSec.size;
        return this.patchedMethodIdSec.writeMethodId(patchedItem);
    }

    @Override
    protected void updateIndexOrOffset(SparseIndexMap sparseIndexMap, int oldIndex, int oldOffset, int newIndex, int newOffset) {
        if (oldIndex != newIndex) {
            sparseIndexMap.mapMethodIds(oldIndex, newIndex);
        }
    }

    @Override
    protected void markDeletedIndexOrOffset(SparseIndexMap sparseIndexMap, int deletedIndex, int deletedOffset) {
        sparseIndexMap.markMethodIdDeleted(deletedIndex);
    }
}
