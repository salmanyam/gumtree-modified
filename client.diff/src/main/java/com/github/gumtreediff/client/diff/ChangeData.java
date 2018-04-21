/*
 * This file is part of GumTree.
 *
 * GumTree is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * GumTree is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with GumTree.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright 2018 Md Salman Ahmed <ahmedms@vt.edu>
 */

package com.github.gumtreediff.client.diff;

public class ChangeData {
    
    private String type;
    private String methodName;
    private String className;
    
    public ChangeData(String type, String className, String methodName) {
        this.type = type;
        this.methodName = methodName;
        this.className = className;
    }

    public String getType() {
        return type;
    }

    public String getMethodName() {
        return methodName;
    }

    public String getClassName() {
        return className;
    }
    
    @Override
    public String toString() {
        return "Type: " + this.type + ", Method: " + this.methodName + ", Class: " + this.className;
    }
}
