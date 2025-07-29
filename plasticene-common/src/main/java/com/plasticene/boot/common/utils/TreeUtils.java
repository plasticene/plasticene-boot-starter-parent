package com.plasticene.boot.common.utils;

import lombok.Data;

import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;

/**
 * @author ZFJ
 * @date 2025/7/28
 */
public class TreeUtils {

    /**
     * 基于空间换时间
     */
    public static <E, R> List<E> listToTreeWithMap(
            List<E> dataList,
            Predicate<E> isRoot,
            Function<E, R> getId,
            Function<E, R> getParentId,
            BiConsumer<E, List<E>> setChildren) {
        // 按父节点分组
        Map<R, List<E>> parentIdToChildren = dataList.stream()
                .filter(node -> !isRoot.test(node))
                .collect(Collectors.groupingBy(getParentId));

        // 设置每个节点的子节点
        dataList.forEach(node -> setChildren.accept(
                node,
                Optional.ofNullable(parentIdToChildren.get(getId.apply(node)))
                        .orElse(Collections.emptyList())));

        // 返回根节点列表
        return dataList.stream()
                .filter(isRoot)
                .collect(Collectors.toList());
    }

    /**
     *
     * @param dataList 数据
     * @param isRoot 判断根节点断言
     * @param isChild 判断子节点断言
     * @param setChildren 设置子树
     * @return tree
     * @param <E> 类型
     */
    public static <E> List<E> listToTree(List<E> dataList,
                                       Predicate<E> isRoot,
                                       BiPredicate<E, E> isChild,
                                       BiConsumer<E, List<E>> setChildren) {
        return dataList.stream().
                // 查找根节点
                filter(isRoot)
                // 给根节点构建子树
                .peek(root -> setChildren.accept(root, buildChildren(root, dataList, isChild, setChildren)))
                .collect(Collectors.toList());
    }

    private static <E> List<E> buildChildren(E parent,
                                            List<E> dataList,
                                            BiPredicate<E, E> isChild,
                                            BiConsumer<E, List<E>> setChildren) {
        return dataList.stream()
                // 获取父节点下的子节点
                .filter(child -> isChild.test(parent, child))
                // 递归调用，构建子节点的子树
                .peek(child -> setChildren.accept(child, buildChildren(child, dataList, isChild, setChildren)))
                .collect(Collectors.toList());
    }

    /**
     * 将Tree转换为List（广度优先）
     * @param tree 树形结构
     * @param getChildren 获取子节点的函数
     * @return 扁平化列表
     */
    public static <E> List<E> treeToListBfs(
            List<E> tree,
            Function<E, List<E>> getChildren) {

        List<E> result = new ArrayList<>();
        Queue<E> queue = new LinkedList<>(tree);

        while (!queue.isEmpty()) {
            E node = queue.poll();
            result.add(node);
            queue.addAll(getChildren.apply(node));
        }

        return result;
    }

    /**
     * 将Tree转换为List（深度优先）
     * @param tree 树形结构
     * @param getChildren 获取子节点的函数
     * @return 扁平化列表
     */
    public static <E> List<E> treeToListDfs(
            List<E> tree,
            Function<E, List<E>> getChildren) {

        List<E> result = new ArrayList<>();
        tree.forEach(node -> traverseDfs(node, getChildren, result));
        return result;
    }

    private static <E> void traverseDfs(
            E node,
            Function<E, List<E>> getChildren,
            List<E> result) {

        result.add(node);
        getChildren.apply(node).forEach(child ->
                traverseDfs(child, getChildren, result));
    }


    public static void main(String[] args) {
        List<Menu> menuList = List.of(
                new Menu(1L, 10L, "m1"),
                new Menu(2L, 9L, "m2"),
                new Menu(3L, 1L, "m3"),
                new Menu(4L, 1L, "m4"),
                new Menu(5L, 2L, "m5"),
                new Menu(6L, 2L, "m6"),
                new Menu(7L, 3L, "m7"),
                new Menu(8L, 3L, "m8"),
                new Menu(9L, 0L, "m9"),
                new Menu(10L, 0L, "m10")
        );

        List<Menu> tree = listToTree(menuList,
                menu -> menu.getParentId() == 0L,
                (parent, child) -> Objects.equals(parent.getId(), child.getParentId()),
                Menu::setChildren);
        List<Menu> tree1 = listToTreeWithMap(menuList,
                menu -> menu.getParentId() == 0,
                Menu::getId,
                Menu::getParentId,
                Menu::setChildren);
        System.out.println(tree);
        System.out.println(tree1);
        List<Menu> list = treeToListBfs(tree, Menu::getChildren);
        System.out.println(list);

    }





    


    @Data
    public static class Menu {
        private Long id;
        private Long parentId;
        private String name;
        private List<Menu> children = new ArrayList<>();

        public Menu(Long id, Long parentId, String name) {
            this.id = id;
            this.parentId = parentId;
            this.name = name;
        }

        public static List<Menu> listToTreeWithMap(List<Menu> dataList) {
            // 空间换时间的体现
            Map<Long, Menu> nodeMap = dataList.stream()
                    .collect(Collectors.toMap(Menu::getId, Function.identity()));

            List<Menu> result = new ArrayList<>();

            // 一次遍历即可，时间复杂度O(n)
            dataList.forEach(menu -> {
                if (menu.getParentId() == 0L) {
                    result.add(menu);
                } else {
                    Menu parent = nodeMap.get(menu.getParentId());
                    if (parent != null) {
                        parent.getChildren().add(menu);
                    }
                }
            });
            return result;
        }

        public static List<Menu> listToTree(List<Menu> dataList) {
            List<Menu> roots = new ArrayList<>();
            // 找出所有根节点 parentId=0的是根节点
            for (Menu menu : dataList) {
                if (menu.getParentId() == 0L) {
                    roots.add(menu);
                }
            }
            // 为每个根节点构建子树
            for (Menu root : roots) {
                buildChildren(root, dataList);
            }
            return roots;
        }

        private static void buildChildren(Menu parent, List<Menu> dataList) {
            //遍历所有数据，获取当前节点的子节点
            for (Menu menu : dataList) {
                if (Objects.equals(parent.getId(), menu.getParentId())) {
                    buildChildren(menu, dataList);
                    //将是当前节点的子节点添加到当前节点的children中
                    parent.getChildren().add(menu);
                }
            }
        }

        public static List<Menu> lisToTree(List<Menu> dataList,
                                          Predicate<Menu> isRoot,
                                          BiPredicate<Menu, Menu> isChild,
                                          BiConsumer<Menu, List<Menu>> setChildren) {
            // 1、获取所有根节点
            List<Menu> roots = dataList.stream().filter(isRoot).collect(Collectors.toList());;
            // 2、所有根节点设置子节点
            roots.forEach(root -> buildChildren(root, dataList, isChild, setChildren));
            return roots;
        }

        private static void buildChildren(Menu parent,
                                         List<Menu> dataList,
                                         BiPredicate<Menu, Menu> isChild,
                                         BiConsumer<Menu, List<Menu>> setChildren) {
            // 找到所有直接子节点
            List<Menu> children = dataList.stream()
                    .filter(child -> isChild.test(parent, child))
                    .collect(Collectors.toList());

            // 递归处理每个子节点
            children.forEach(child -> buildChildren(child, dataList, isChild, setChildren));

            // 将子节点列表设置到父节点
            setChildren.accept(parent, children);
        }

    }



}
