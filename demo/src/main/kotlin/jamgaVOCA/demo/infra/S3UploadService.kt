package jamgaVOCA.demo.infra

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import java.util.Base64
import java.util.UUID

@Service
class S3UploadService(
    private val s3Client: S3Client,
    @Value($$"${cloud.aws.s3.bucket}") private val bucket: String
) {
    fun uploadBase64Image(base64: String, folder: String = "skills"): String {
        val bytes = Base64.getDecoder().decode(base64)
        val key = "$folder/${UUID.randomUUID()}.png"

        s3Client.putObject(
            PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType("image/png")
                .build(),
            RequestBody.fromBytes(bytes)
        )

        return "https://$bucket.s3.ap-northeast-2.amazonaws.com/$key"
    }
}