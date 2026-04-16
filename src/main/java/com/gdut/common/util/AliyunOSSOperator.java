package com.gdut.common.util;

import com.aliyun.oss.*;
import com.aliyun.oss.common.auth.CredentialsProviderFactory;
import com.aliyun.oss.common.auth.EnvironmentVariableCredentialsProvider;
import com.aliyun.oss.common.comm.SignVersion;
import com.aliyun.oss.model.PutObjectRequest;
import com.aliyun.oss.model.PutObjectResult;
import com.gdut.domain.entity.common.AliyunOSSProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class AliyunOSSOperator {

    private final AliyunOSSProperties aliyunOSSProperties;

    public String upload(MultipartFile file, String originalFilename) throws Exception {
        String endpoint = aliyunOSSProperties.getEndpoint();
        String bucketName = aliyunOSSProperties.getBucketName();
        String region = aliyunOSSProperties.getRegion();

        // 从环境变量中获取访问凭证。运行本代码示例之前，请确保已设置环境变量OSS_ACCESS_KEY_ID和OSS_ACCESS_KEY_SECRET。
        EnvironmentVariableCredentialsProvider credentialsProvider = CredentialsProviderFactory.newEnvironmentVariableCredentialsProvider();

        // 填写 Object 完整路径，例如 202406/1.png。Object 完整路径中不能包含 Bucket 名称
        // 获取当前系统日期的字符串，格式为 yyyy/MM
        String dir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
        //生成一个新的不重复的文件名
        String newFileName = UUID.randomUUID() + originalFilename.substring(originalFilename.lastIndexOf("."));
        String objectName = dir + "/" + newFileName;



        // 创建OSSClient实例。
        // 当OSSClient实例不再使用时，调用shutdown方法以释放资源。
        ClientBuilderConfiguration clientBuilderConfiguration = new ClientBuilderConfiguration();
        clientBuilderConfiguration.setSignatureVersion(SignVersion.V4);
        OSS ossClient = OSSClientBuilder.create()
                .endpoint(endpoint)
                .credentialsProvider(credentialsProvider)
                .clientConfiguration(clientBuilderConfiguration)
                .region(region)
                .build();

        try {
            InputStream inputStream = file.getInputStream();
            // 创建 PutObjectRequest 对象。
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, objectName, inputStream);
            // 创建 PutObject 请求。
            PutObjectResult result = ossClient.putObject(putObjectRequest);
        } catch (OSSException oe) {
            log.error("上传文件失败，OSS 异常：{}", oe.getErrorMessage());
            throw oe;
        } catch (ClientException ce) {
            log.error("上传文件失败，客户端异常：{}", ce.getMessage());
            throw ce;
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
        return "https://" + bucketName + "." + endpoint.split("//")[1] + "/" + objectName;
    }

    /**
     * 删除 OSS 文件
     * @param objectName OSS 文件路径
     */
    public void deleteFile(String objectName) throws Exception {
        String endpoint = aliyunOSSProperties.getEndpoint();
        String bucketName = aliyunOSSProperties.getBucketName();
        String region = aliyunOSSProperties.getRegion();

        EnvironmentVariableCredentialsProvider credentialsProvider = CredentialsProviderFactory.newEnvironmentVariableCredentialsProvider();

        ClientBuilderConfiguration clientBuilderConfiguration = new ClientBuilderConfiguration();
        clientBuilderConfiguration.setSignatureVersion(SignVersion.V4);

        OSS ossClient = OSSClientBuilder.create()
                .endpoint(endpoint)
                .credentialsProvider(credentialsProvider)
                .clientConfiguration(clientBuilderConfiguration)
                .region(region)
                .build();

        try {
            ossClient.deleteObject(bucketName, objectName);
            log.info("成功删除 OSS 文件：{}/{}", bucketName, objectName);
        } catch (OSSException oe) {
            log.error("删除 OSS 文件失败，OSS 异常：{}", oe.getErrorMessage());
            throw oe;
        } catch (ClientException ce) {
            log.error("删除 OSS 文件失败，客户端异常：{}", ce.getMessage());
            throw ce;
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }

    /**
     * 从完整 URL 中提取 OSS Object 路径
     * @param url OSS 文件完整 URL
     * @return Object 路径
     */
    public String extractObjectKeyFromUrl(String url) {
        if (url == null || url.isEmpty()) {
            return null;
        }
        int index = 0;
        int count = 0;
        for (int i = 0; i < url.length(); i++) {
            if (url.charAt(i) == '/') {
                count++;
                if (count == 3) {
                    index = i + 1;
                    break;
                }
            }
        }
        return url.substring(index);
    }
}
