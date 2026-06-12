package com.hmdp.contract;

import com.hmdp.controller.BlogController;
import com.hmdp.controller.FollowController;
import com.hmdp.controller.ShopController;
import com.hmdp.controller.ShopTypeController;
import com.hmdp.controller.UploadController;
import com.hmdp.controller.UserController;
import com.hmdp.controller.VoucherController;
import com.hmdp.controller.VoucherOrderController;
import com.hmdp.dto.Result;
import org.junit.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ApiContractTest {

    @Test
    public void resultShapeShouldRemainCompatibleWithFrontendInterceptor() throws Exception {
        assertField(Result.class, "success", Boolean.class);
        assertField(Result.class, "errorMsg", String.class);
        assertField(Result.class, "data", Object.class);
        assertField(Result.class, "total", Long.class);
    }

    @Test
    public void userApiMappingsShouldRemainStable() throws Exception {
        assertBasePath(UserController.class, "/user");
        assertPost(UserController.class, "sendCode", "code");
        assertPost(UserController.class, "login", "/login");
        assertPost(UserController.class, "logout", "/logout");
        assertGet(UserController.class, "me", "/me");
        assertGet(UserController.class, "info", "/info/{id}");
        assertGet(UserController.class, "queryUserById", "/{id}");
    }

    @Test
    public void shopAndVoucherApiMappingsShouldRemainStable() throws Exception {
        assertBasePath(ShopTypeController.class, "/shop-type");
        assertGet(ShopTypeController.class, "queryTypeList", "list");

        assertBasePath(ShopController.class, "/shop");
        assertGet(ShopController.class, "queryShopById", "/{id}");
        assertPost(ShopController.class, "saveShop", "");
        assertPut(ShopController.class, "updateShop", "");
        assertGet(ShopController.class, "queryShopByType", "/of/type");
        assertGet(ShopController.class, "queryShopByName", "/of/name");

        assertBasePath(VoucherController.class, "/voucher");
        assertGet(VoucherController.class, "queryVoucherOfShop", "/list/{shopId}");

        assertBasePath(VoucherOrderController.class, "/voucher-order");
        assertPost(VoucherOrderController.class, "seckillVoucher", "seckill/{id}");
    }

    @Test
    public void blogAndFollowApiMappingsShouldRemainStable() throws Exception {
        assertBasePath(BlogController.class, "/blog");
        assertPost(BlogController.class, "saveBlog", "");
        assertPut(BlogController.class, "likeBlog", "/like/{id}");
        assertGet(BlogController.class, "queryMyBlog", "/of/me");
        assertGet(BlogController.class, "queryHotBlog", "/hot");
        assertGet(BlogController.class, "queryBlog", "/{id}");
        assertGet(BlogController.class, "queryLikes", "/likes/{id}");
        assertGet(BlogController.class, "queryBlogOfFollow", "/of/follow");
        assertGet(BlogController.class, "queryBlogByUserId", "/of/user");

        assertBasePath(FollowController.class, "/follow");
        assertPut(FollowController.class, "follow", "/{id}/{isFollow}");
        assertGet(FollowController.class, "isFollow", "/or/not/{id}");
        assertGet(FollowController.class, "followCommons", "/common/{id}");
    }

    @Test
    public void uploadApiMappingsShouldRemainStable() throws Exception {
        assertBasePath(UploadController.class, "upload");
        assertPost(UploadController.class, "uploadImage", "blog");
        assertGet(UploadController.class, "deleteBlogImg", "/blog/delete");
    }

    private static void assertField(Class<?> type, String name, Class<?> fieldType) throws Exception {
        Field field = type.getDeclaredField(name);
        assertEquals(fieldType, field.getType());
    }

    private static void assertBasePath(Class<?> controllerClass, String expected) {
        RequestMapping mapping = controllerClass.getAnnotation(RequestMapping.class);
        assertNotNull("Missing @RequestMapping on " + controllerClass.getSimpleName(), mapping);
        assertTrue(Arrays.asList(mapping.value()).contains(expected));
    }

    private static void assertGet(Class<?> controllerClass, String methodName, String expectedPath) throws Exception {
        Method method = findMethod(controllerClass, methodName);
        GetMapping mapping = method.getAnnotation(GetMapping.class);
        assertNotNull("Missing @GetMapping on " + controllerClass.getSimpleName() + "." + methodName, mapping);
        assertTrue("Unexpected GET path for " + methodName, containsPath(mapping.value(), expectedPath));
    }

    private static void assertPost(Class<?> controllerClass, String methodName, String expectedPath) throws Exception {
        Method method = findMethod(controllerClass, methodName);
        PostMapping mapping = method.getAnnotation(PostMapping.class);
        assertNotNull("Missing @PostMapping on " + controllerClass.getSimpleName() + "." + methodName, mapping);
        assertTrue("Unexpected POST path for " + methodName, containsPath(mapping.value(), expectedPath));
    }

    private static void assertPut(Class<?> controllerClass, String methodName, String expectedPath) throws Exception {
        Method method = findMethod(controllerClass, methodName);
        PutMapping mapping = method.getAnnotation(PutMapping.class);
        assertNotNull("Missing @PutMapping on " + controllerClass.getSimpleName() + "." + methodName, mapping);
        assertTrue("Unexpected PUT path for " + methodName, containsPath(mapping.value(), expectedPath));
    }

    private static Method findMethod(Class<?> controllerClass, String methodName) {
        return Arrays.stream(controllerClass.getDeclaredMethods())
                .filter(method -> method.getName().equals(methodName))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing method " + controllerClass.getSimpleName() + "." + methodName));
    }

    private static boolean containsPath(String[] paths, String expectedPath) {
        if ("".equals(expectedPath) && paths.length == 0) {
            return true;
        }
        return Arrays.asList(paths).contains(expectedPath);
    }
}
