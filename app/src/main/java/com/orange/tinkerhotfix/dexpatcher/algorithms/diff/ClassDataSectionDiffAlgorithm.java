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
import com.orange.tinkerhotfix.party.ClassData;
import com.orange.tinkerhotfix.party.Dex;
import com.orange.tinkerhotfix.party.SizeOf;
import com.orange.tinkerhotfix.party.TableOfContents;
import com.orange.tinkerhotfix.party.io.DexDataBuffer;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by tangyinsheng on 2016/6/30.
 */
public class ClassDataSectionDiffAlgorithm extends DexSectionDiffAlgorithm<ClassData> {
    private Set<Integer> offsetOfClassDataToRemoveSet = new HashSet<>();

    public ClassDataSectionDiffAlgorithm(Dex oldDex, Dex newDex, SparseIndexMap oldToNewIndexMap, SparseIndexMap oldToPatchedIndexMap, SparseIndexMap newToPatchedIndexMap, SparseIndexMap selfIndexMapForSkip) {
        super(oldDex, newDex, oldToNewIndexMap, oldToPatchedIndexMap, newToPatchedIndexMap, selfIndexMapForSkip);
    }

    public void setOffsetOfClassDatasToRemove(Collection<Integer> offsetOfClassDatasToRemove) {
        this.offsetOfClassDataToRemoveSet.clear();
        this.offsetOfClassDataToRemoveSet.addAll(offsetOfClassDatasToRemove);
    }

    public void clearTypeIdOfClassDefsToRemove() {
        this.offsetOfClassDataToRemoveSet.clear();
    }

    @Override
    protected TableOfContents.Section getTocSection(Dex dex) {
        return dex.getTableOfContents().classDatas;
    }

    @Override
    protected ClassData nextItem(DexDataBuffer section) {
        return section.readClassData();
    }

    @Override
    protected int getItemSize(ClassData item) {
        return item.byteCountInDex();
    }

    @Override
    protected ClassData adjustItem(AbstractIndexMap indexMap, ClassData item) {
        return indexMap.adjust(item);
    }

    @Override
    public int getPatchedSectionSize() {
        // assume each uleb128 field's length may be inflate by 2 bytes.
        return super.getPatchedSectionSize() + newDex.getTableOfContents().classDatas.size * SizeOf.USHORT;
    }

    @Override
    protected boolean shouldSkipInNewDex(ClassData newItem) {
        return this.offsetOfClassDataToRemoveSet.contains(newItem.off);
    }

    @Override
    protected void updateIndexOrOffset(SparseIndexMap sparseIndexMap, int oldIndex, int oldOffset, int newIndex, int newOffset) {
        if (oldOffset != newOffset) {
            sparseIndexMap.mapClassDataOffset(oldOffset, newOffset);
        }
    }

    @Override
    protected void markDeletedIndexOrOffset(SparseIndexMap sparseIndexMap, int deletedIndex, int deletedOffset) {
        sparseIndexMap.markClassDataDeleted(deletedOffset);
    }
}
