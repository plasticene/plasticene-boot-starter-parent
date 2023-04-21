package com.plasticene.boot.oss.core;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.amazonaws.util.IOUtils;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

/**
 * @author fjzheng
 * @version 1.0
 * @date 2023/4/20 13:52
 */
@RequiredArgsConstructor
public class OssClient implements OssTemplate{

    private final AmazonS3 amazonS3;

    /**
     * 创建Bucket
     * AmazonS3：https://docs.aws.amazon.com/AmazonS3/latest/API/API_CreateBucket.html
     * @param bucketName bucket名称
     */
    @Override
    @SneakyThrows
    public void createBucket(String bucketName) {
        if ( !amazonS3.doesBucketExistV2(bucketName) ) {
            amazonS3.createBucket((bucketName));
        }
    }

    /**
     * 获取所有的buckets
     * AmazonS3：https://docs.aws.amazon.com/AmazonS3/latest/API/API_ListBuckets.html
     * @return
     */
    @Override
    @SneakyThrows
    public List<Bucket> getAllBuckets() {
        return amazonS3.listBuckets();
    }

    /**
     * 通过Bucket名称删除Bucket
     * AmazonS3：https://docs.aws.amazon.com/AmazonS3/latest/API/API_DeleteBucket.html
     * @param bucketName
     */
    @Override
    @SneakyThrows
    public void removeBucket(String bucketName) {
        amazonS3.deleteBucket(bucketName);
    }

    /**
     * 上传对象
     * @param bucketName bucket名称
     * @param objectName 文件名称
     * @param stream 文件流
     * @param contextType 文件类型
     * AmazonS3：https://docs.aws.amazon.com/AmazonS3/latest/API/API_PutObject.html
     */
    @Override
    @SneakyThrows
    public void putObject(String bucketName, String objectName, InputStream stream, String contextType) {
        putObject(bucketName, objectName, stream, stream.available(), contextType);
    }
    /**
     * 上传对象
     * @param bucketName bucket名称
     * @param objectName 文件名称
     * @param stream 文件流
     * AmazonS3：https://docs.aws.amazon.com/AmazonS3/latest/API/API_PutObject.html
     */
    @Override
    @SneakyThrows
    public void putObject(String bucketName, String objectName, InputStream stream) {
        putObject(bucketName, objectName, stream, stream.available(), "application/octet-stream");
    }

    /**
     * 通过bucketName和objectName获取对象
     * @param bucketName bucket名称
     * @param objectName 文件名称
     * @return
     * AmazonS3：https://docs.aws.amazon.com/AmazonS3/latest/API/API_GetObject.html
     */
    @Override
    @SneakyThrows
    public S3Object getObject(String bucketName, String objectName) {
        return amazonS3.getObject(bucketName, objectName);
    }

    /**
     * 获取对象的url
     * @param bucketName
     * @param objectName
     * @param expires
     * @return
     * AmazonS3：https://docs.aws.amazon.com/AmazonS3/latest/API/API_GeneratePresignedUrl.html
     */
    @Override
    @SneakyThrows
    public String getObjectURL(String bucketName, String objectName, Integer expires) {
        Date date = new Date();
        Calendar calendar = new GregorianCalendar();
        calendar.setTime(date);
        calendar.add(Calendar.DAY_OF_MONTH, expires);
        URL url = amazonS3.generatePresignedUrl(bucketName, objectName, calendar.getTime());
        return url.toString();
    }

    /**
     * 通过bucketName和objectName删除对象
     * @param bucketName
     * @param objectName
     * AmazonS3：https://docs.aws.amazon.com/AmazonS3/latest/API/API_DeleteObject.html
     */
    @Override
    @SneakyThrows
    public void removeObject(String bucketName, String objectName) {
        amazonS3.deleteObject(bucketName, objectName);
    }

    /**
     * 根据bucketName和prefix获取对象集合
     * @param bucketName bucket名称
     * @param prefix 前缀
     * @param recursive 是否递归查询
     * @return
     * AmazonS3：https://docs.aws.amazon.com/AmazonS3/latest/API/API_ListObjects.html
     */
    @Override
    @SneakyThrows
    public List<S3ObjectSummary> getAllObjectsByPrefix(String bucketName, String prefix, boolean recursive) {
        ObjectListing objectListing = amazonS3.listObjects(bucketName, prefix);
        return objectListing.getObjectSummaries();
    }


    /**
     *
     * @param bucketName
     * @param objectName
     * @param stream
     * @param size
     * @param contextType
     * @return
     */
    @SneakyThrows
    private PutObjectResult putObject(String bucketName, String objectName, InputStream stream, long size,
                                      String contextType)  {

        byte[] bytes = IOUtils.toByteArray(stream);
        ObjectMetadata objectMetadata = new ObjectMetadata();
        objectMetadata.setContentLength(size);
        objectMetadata.setContentType(contextType);
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
        // 上传
        return amazonS3.putObject(bucketName, objectName, byteArrayInputStream, objectMetadata);

    }

}
