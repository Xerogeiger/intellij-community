// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.filePrediction.features

import com.intellij.filePrediction.features.FilePredictionFeature.Companion.binary
import com.intellij.filePrediction.features.FilePredictionFeature.Companion.fileType
import com.intellij.filePrediction.features.FilePredictionFeature.Companion.numerical
import com.intellij.filePrediction.references.ExternalReferencesResult
import com.intellij.internal.statistic.collectors.fus.fileTypes.FileTypeUsagesCollector
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.roots.FileIndexFacade
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.util.text.StringUtil.toLowerCase
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightVirtualFile
import com.intellij.util.PathUtil
import com.intellij.util.ThreeState
import java.io.File

class FilePredictionGeneralFeatures: FilePredictionFeatureProvider {
  companion object {
    private val FEATURES = arrayListOf(
      "excluded",
      "file_type",
      "in_library",
      "in_project",
      "in_ref",
      "in_source",
      "light",
      "name_prefix",
      "path_prefix",
      "prev_file_type",
      "relative_path_prefix",
      "same_dir",
      "same_module"
    )
  }

  override fun getName(): String = "core"

  override fun getFeatures(): List<String> = FEATURES

  override fun calculateFileFeatures(project: Project,
                                     newFile: VirtualFile,
                                     prevFile: VirtualFile?,
                                     refs: ExternalReferencesResult): Map<String, FilePredictionFeature> {
    val result = HashMap<String, FilePredictionFeature>()
    val isInRef = refs.contains(newFile)
    if (isInRef != ThreeState.UNSURE) {
      result["in_ref"] = binary(isInRef == ThreeState.YES)
    }

    addFileTypeFeatures(result, newFile, prevFile)
    ApplicationManager.getApplication().runReadAction {
      if (newFile.isValid) {
        val fileIndex = FileIndexFacade.getInstance(project)
        result["in_project"] = binary(fileIndex.isInProjectScope(newFile))
        result["in_source"] = binary(fileIndex.isInSource(newFile))
        result["in_library"] = binary(fileIndex.isInLibraryClasses(newFile) || fileIndex.isInLibrarySource(newFile))
        result["excluded"] = binary(fileIndex.isExcludedFile(newFile))

        if (prevFile != null && prevFile.isValid) {
          val newModule = fileIndex.getModuleForFile(newFile)
          result["same_module"] = binary(newModule != null && newModule == fileIndex.getModuleForFile(prevFile))
        }
        result["light"] = binary(newFile is LightVirtualFile)
      }
    }

    if (prevFile != null) {
      val newFileName = unify(newFile.name)
      val newFilePath = unify(newFile.path)
      val prevFileName = unify(prevFile.name)
      result["name_prefix"] = numerical(StringUtil.commonPrefixLength(newFileName, prevFileName))

      val prevFilePath = unify(prevFile.path)
      result["path_prefix"] = numerical(StringUtil.commonPrefixLength(newFilePath, prevFilePath))

      if (!project.isDisposed) {
        val baseDir = project.guessProjectDir()?.path?.let { unify(it) }
        if (baseDir != null) {
          val newRelativePath = FileUtil.getRelativePath(baseDir, newFilePath, File.separatorChar, false)
          val prevRelativePath = FileUtil.getRelativePath(baseDir, prevFilePath, File.separatorChar, false)
          if (newRelativePath != null && prevRelativePath != null) {
            result["relative_path_prefix"] = numerical(StringUtil.commonPrefixLength(newRelativePath, prevRelativePath))
          }
        }
      }
      result["same_dir"] = binary(PathUtil.getParentPath(newFilePath) == PathUtil.getParentPath(prevFilePath))
    }
    return result
  }

  private fun addFileTypeFeatures(result: MutableMap<String, FilePredictionFeature>, newFile: VirtualFile, prevFile: VirtualFile?) {
    result["file_type"] = fileType(FileTypeUsagesCollector.getSafeFileTypeName(newFile.fileType))

    if (prevFile != null) {
      result["prev_file_type"] = fileType(FileTypeUsagesCollector.getSafeFileTypeName(prevFile.fileType))
    }
  }

  private fun unify(path: String) : String {
    val caseSensitive = SystemInfo.isFileSystemCaseSensitive
    return if (caseSensitive) FileUtil.getNameWithoutExtension(path) else FileUtil.getNameWithoutExtension(toLowerCase(path))
  }
}
