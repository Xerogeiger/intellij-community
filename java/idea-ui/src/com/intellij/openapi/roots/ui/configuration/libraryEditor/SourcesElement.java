/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.openapi.roots.ui.configuration.libraryEditor;

import com.intellij.ide.util.treeView.NodeDescriptor;
import com.intellij.openapi.roots.OrderRootType;

class SourcesElement extends LibraryTableTreeContentElement {
    private final LibraryElement myParent;

    public SourcesElement(LibraryElement parent) {
      myParent = parent;
    }

    public LibraryElement getParent() {
      return myParent;
    }

  public OrderRootType getOrderRootType() {
    return OrderRootType.SOURCES;
  }

  public NodeDescriptor createDescriptor(final NodeDescriptor parentDescriptor, final LibraryRootsComponent parentEditor) {
    return new SourcesElementDescriptor(parentDescriptor, this);
  }

  public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof SourcesElement)) return false;

      final SourcesElement sourcesElement = (SourcesElement)o;

      if (!myParent.equals(sourcesElement.myParent)) return false;

      return true;
    }

    public int hashCode() {
      return myParent.hashCode();
    }
  }
