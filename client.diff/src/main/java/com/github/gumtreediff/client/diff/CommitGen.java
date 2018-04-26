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


import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.swing.tree.DefaultMutableTreeNode;

import com.github.gumtreediff.actions.RootsClassifier;
import com.github.gumtreediff.actions.TreeClassifier;
import com.github.gumtreediff.client.Register;
import com.github.gumtreediff.matchers.Matcher;
import com.github.gumtreediff.tree.ITree;
import com.github.gumtreediff.tree.TreeContext;
import com.github.gumtreediff.utils.Tuple;

@Register(description = "A commit generator class from diff", options = AbstractDiffClient.Options.class)
public final class CommitGen extends AbstractDiffClient<AbstractDiffClient.Options> {
    private enum DiffType {
        SRC_DEL,
        DST_INS,
        SRC_UPD,
        DST_UPD,
        SRC_MOV,
        DST_MOV
    }
    
    TreeClassifier classifyTrees = null;
    
    private Set<ChangeData> insertedItems = null;
    private Set<ChangeData> updateItems = null;
    private Set<ChangeData> deletedItems = null;
    
    private Map<Integer, Boolean> srcVisited;
    private Map<Integer, Boolean> dstVisited;
    
    public CommitGen(String[] args) {
        super(args);
        srcVisited = new HashMap<>();
        dstVisited = new HashMap<>();
        insertedItems = new HashSet<>();
        updateItems = new HashSet<>();
        deletedItems = new HashSet<>();
    }
    
    @Override
    public void run() {
        final Matcher matcher = matchTrees();
        classifyTrees = new RootsClassifier(getSrcTreeContext(), getDstTreeContext(), matcher);
        
        // Operation on the destination tree for insertion and update
        TreeContext dstTree = getDstTreeContext();
        ITree dstRoot = dstTree.getRoot();
        DefaultMutableTreeNode dstTop = new DefaultMutableTreeNode(dstRoot);

        String className = "";
        String methodName = "";
        String fieldName = "";
        
        dstVisited.put(dstRoot.getId(), true);
        
        // Look for classes from the root of an AST 
        for (ITree child: dstRoot.getChildren()) {
            // Recursively finding changes for only a class
            if (child.toPrettyString(getDstTreeContext()).equals("TypeDeclaration")) {
                for (ITree c : child.getChildren()) {
                    if (!c.hasLabel()) continue;
                    if (getDstTreeContext().getTypeLabel(c).equalsIgnoreCase("SimpleName")) {
                        className = c.getLabel();
                        break;
                    }
                }
                // Initiates a recursive call
                createDstNodes(dstTop, child, className, methodName, fieldName, 0);
            }
        }
        
        // Operation on the source tree for deletion
        TreeContext srcTree = getSrcTreeContext();
        ITree srcRoot = srcTree.getRoot();
        DefaultMutableTreeNode srcTop = new DefaultMutableTreeNode(srcRoot);

        className = "";
        methodName = "";
        fieldName = "";
        
        srcVisited.put(srcRoot.getId(), true);
        
        // Look for classes from the root of an AST 
        for (ITree child: srcRoot.getChildren()) {
            if (child.toPrettyString(getSrcTreeContext()).equals("TypeDeclaration")) {
                for (ITree c : child.getChildren()) {
                    if (!c.hasLabel()) continue;
                    if (getSrcTreeContext().getTypeLabel(c).equalsIgnoreCase("SimpleName")) {
                        className = c.getLabel();
                        srcTop = new DefaultMutableTreeNode(c);
                    }
                }
                // Initiates a recursive call
                createSrcNodes(srcTop, child, className, methodName, fieldName, 0);
            }
            
        }
    }
    
    public Set<ChangeData> getInsertedItems() {
        return this.insertedItems;
    }
    
    public Set<ChangeData> getUpdatedItems() {
        return this.updateItems;
    }
    
    public Set<ChangeData> getDeletedItems() {
        return this.deletedItems;
    }

    private void createDstNodes(DefaultMutableTreeNode parent, 
              ITree tree, String className, String methodName, String fieldName, int space) {
        if (tree == null) return;
        
        if (tree.toPrettyString(getDstTreeContext()).equals("Javadoc"))
            return;
        
        if (dstVisited.containsKey(tree.getId())) return;
        
        
        String format = "";
        for (int i = 0; i < space; i++) {
            format += "\t";
        }
        
        String r = "";
        if (classifyTrees.getDstAddTrees().contains(tree)) {
            r = " DST :DST ADD";
            //System.out.println(format + tree.toPrettyString(getDstTreeContext()) + r);
            processDstAdd(tree, className, methodName, fieldName);
            return;
        }
        else if (classifyTrees.getSrcUpdTrees().contains(tree)) r = " DST :SRC UPD";
        else if (classifyTrees.getDstUpdTrees().contains(tree)) {
            r = " DST :DST UPd";
            //System.out.println(format + tree.toPrettyString(getDstTreeContext()) + r);
            processDstUpd(tree, className, methodName, fieldName);
            return;
        }
        else if (classifyTrees.getSrcMvTrees().contains(tree)) r = " DST :SRC MOV";
        else if (classifyTrees.getDstMvTrees().contains(tree)) r = " DST :DST MOV";
        
        //System.out.println(format + tree.toPrettyString(getDstTreeContext()) + r);
        
        dstVisited.put(tree.getId(), true);
        
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(tree);
        parent.add(node);
        
        for (ITree child: tree.getChildren()) {
            
            if (child.toPrettyString(getDstTreeContext()).equals("MethodDeclaration")) {
                String mname = "";
                for (ITree c : child.getChildren()) {
                    if (!c.hasLabel()) continue;
                    if (getDstTreeContext().getTypeLabel(c).equalsIgnoreCase("SimpleName")) {
                        mname = c.getLabel();
                        break;
                    }
                }
                createDstNodes(node, child, className, mname, fieldName, space + 1);
            } else if (child.toPrettyString(getDstTreeContext()).equals("FieldDeclaration")) {
                String fname = "";
                for (ITree c : child.getChildren()) {
                    if (getDstTreeContext().getTypeLabel(c).equalsIgnoreCase("VariableDeclarationFragment")) {
                        for (ITree cc : c.getChildren()) {
                            if (!cc.hasLabel()) continue;
                            if (getDstTreeContext().getTypeLabel(cc).equalsIgnoreCase("SimpleName")) {
                                fname = cc.getLabel();
                                break;
                            }
                        }
                    }
                }
                createDstNodes(node, child, className, methodName, fname, space + 1);
            }
            else
                createDstNodes(node, child, className, methodName, fieldName, space + 1);
        }
    }

    private void createSrcNodes(DefaultMutableTreeNode parent, 
            ITree tree, String className, String methodName, String fieldName, int space) {
        if (tree == null) return;

        if (tree.toPrettyString(getSrcTreeContext()).equals("Javadoc"))
            return;
        
        if (srcVisited.containsKey(tree.getId())) return;
        
        String format = "";
        for (int i = 0; i < space; i++) {
            format += "\t";
        }
        
        String r = "";
        if (classifyTrees.getSrcDelTrees().contains(tree)) {
            r = " SRC :SRC DEL";
            //System.out.println(format + tree.toPrettyString(getSrcTreeContext()) + r);
            processSrcDelete(tree, className, methodName, fieldName);
            return;
        }
        else if (classifyTrees.getSrcUpdTrees().contains(tree)) r = " SRC :SRC UPD";
        else if (classifyTrees.getDstUpdTrees().contains(tree)) r = " SRC :DST UPd";
        else if (classifyTrees.getSrcMvTrees().contains(tree)) r = " SRC :SRC MOV";
        else if (classifyTrees.getDstMvTrees().contains(tree)) r = " SRC :DST MOV";
        
        
        //System.out.println(format + tree.toPrettyString(getSrcTreeContext()) + r);
        
        srcVisited.put(tree.getId(), true);
        
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(tree);
        parent.add(node);
        
        for (ITree child: tree.getChildren())
            if (child.toPrettyString(getSrcTreeContext()).equals("MethodDeclaration")) {
                String mname = "";
                for (ITree c : child.getChildren()) {
                    if (!c.hasLabel()) continue;
                    if (getSrcTreeContext().getTypeLabel(c).equalsIgnoreCase("SimpleName")) {
                        mname = c.getLabel();
                        break;
                    }
                }
                createSrcNodes(node, child, className, mname, fieldName, space + 1);
            } else if (child.toPrettyString(getSrcTreeContext()).equals("FieldDeclaration")) {
                String fname = "";
                for (ITree c : child.getChildren()) {
                    if (getSrcTreeContext().getTypeLabel(c).equalsIgnoreCase("VariableDeclarationFragment")) {
                        for (ITree cc : c.getChildren()) {
                            if (!cc.hasLabel()) continue;
                            if (getSrcTreeContext().getTypeLabel(cc).equalsIgnoreCase("SimpleName")) {
                                fname = cc.getLabel();
                                break;
                            }
                        }
                    }
                }
                createSrcNodes(node, child, className, methodName, fname, space + 1);
            }
            else
                createSrcNodes(node, child, className, methodName, fieldName, space + 1);
    }
    
    private void processDstAdd(ITree treeNode, String className, String methodName, String fieldName) {
        //System.out.println("salmaninsert " + treeNode.toPrettyString(getDstTreeContext()) 
          //                 + " " + className + " " + methodName);
        insertedItems.add(new ChangeData(
                treeNode.toPrettyString(getDstTreeContext()), 
                className,
                methodName,
                fieldName));
        
        /*DefaultMutableTreeNode top = new DefaultMutableTreeNode(treeNode);
        for (ITree child: treeNode.getChildren()) {
            visitDstNodes(top, child);
        }*/
    }
    
    private void processDstUpd(ITree treeNode, String className, String methodName, String fieldName) {
        //System.out.println("salmanupdate " + treeNode.toPrettyString(getDstTreeContext()) 
          //                 + " " + className + " " + methodName);
        updateItems.add(new ChangeData(
                treeNode.toPrettyString(getDstTreeContext()), 
                className,
                methodName,
                fieldName));
        
        /*DefaultMutableTreeNode top = new DefaultMutableTreeNode(treeNode);
        for (ITree child: treeNode.getChildren()) {
            visitDstNodes(top, child);
        }*/
    }
    
    private void processSrcDelete(ITree treeNode, String className, String methodName, String fieldName) {
        //System.out.println("salmandelete " + treeNode.toPrettyString(getSrcTreeContext()) + " " 
          //                 + className + " " + methodName);
        deletedItems.add(new ChangeData(
                treeNode.toPrettyString(getSrcTreeContext()), 
                className,
                methodName,
                fieldName));
        
        /*DefaultMutableTreeNode top = new DefaultMutableTreeNode(treeNode);
        for (ITree child: treeNode.getChildren()) {
            visitSrcNodes(top, child);
        }*/
    }
    /*
    private void processOperation(ITree treeNode, String operationType, String className, String methodName) {
        if (operationType.equalsIgnoreCase("INS")) {
            insertedItems.add(new Tetraple<String, String, String, String>(
                    treeNode.toPrettyString(getDstTreeContext()), 
                    treeNode.getParent().toPrettyString(getDstTreeContext()),
                    className,
                    methodName));
        } else if (operationType.equalsIgnoreCase("UPD")) {
            updateItems.add(new Tetraple<String, String, String, String>(
                    treeNode.toPrettyString(getDstTreeContext()), 
                    treeNode.getParent().toPrettyString(getDstTreeContext()),
                    className,
                    methodName));
        
        } else if (operationType.equalsIgnoreCase("DEL")) {
            deletedItems.add(new Tetraple<String, String, String, String>(
                    treeNode.toPrettyString(getSrcTreeContext()), 
                    "",
                    className,
                    methodName));
        }
        
        DefaultMutableTreeNode top = new DefaultMutableTreeNode(treeNode);
        for (ITree child: treeNode.getChildren()) {
            if (operationType.equalsIgnoreCase("DEL"))
                visitSrcNodes(top, child);
            else
                visitDstNodes(top, child);
        }
    }
    
    private void visitDstNodes(DefaultMutableTreeNode parent, ITree tree) {
        if (tree == null) return;
        
        dstVisited.put(tree.getId(), true);
        
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(tree);
        parent.add(node);
        for (ITree child: tree.getChildren())
            visitDstNodes(node, child);
    }
    
    private void visitSrcNodes(DefaultMutableTreeNode parent, ITree tree) {
        if (tree == null) return;
        
        srcVisited.put(tree.getId(), true);
        
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(tree);
        parent.add(node);
        for (ITree child: tree.getChildren())
            visitSrcNodes(node, child);
    }*/

    @Override
    protected AbstractDiffClient.Options newOptions() {
        return new AbstractDiffClient.Options();
    }
}