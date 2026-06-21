package com.talex.server.services.impls;

import com.talex.server.configs.properties.MediaProperties;
import com.talex.server.entities.Media;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.mediaconvert.MediaConvertClient;
import software.amazon.awssdk.services.mediaconvert.model.AacCodingMode;
import software.amazon.awssdk.services.mediaconvert.model.AacSettings;
import software.amazon.awssdk.services.mediaconvert.model.AudioCodecSettings;
import software.amazon.awssdk.services.mediaconvert.model.AudioDefaultSelection;
import software.amazon.awssdk.services.mediaconvert.model.AudioDescription;
import software.amazon.awssdk.services.mediaconvert.model.AudioSelector;
import software.amazon.awssdk.services.mediaconvert.model.ContainerSettings;
import software.amazon.awssdk.services.mediaconvert.model.ContainerType;
import software.amazon.awssdk.services.mediaconvert.model.CreateJobRequest;
import software.amazon.awssdk.services.mediaconvert.model.CreateJobResponse;
import software.amazon.awssdk.services.mediaconvert.model.FileGroupSettings;
import software.amazon.awssdk.services.mediaconvert.model.FrameCaptureSettings;
import software.amazon.awssdk.services.mediaconvert.model.GetJobRequest;
import software.amazon.awssdk.services.mediaconvert.model.GetJobResponse;
import software.amazon.awssdk.services.mediaconvert.model.HlsCaptionLanguageSetting;
import software.amazon.awssdk.services.mediaconvert.model.HlsClientCache;
import software.amazon.awssdk.services.mediaconvert.model.HlsCodecSpecification;
import software.amazon.awssdk.services.mediaconvert.model.HlsDirectoryStructure;
import software.amazon.awssdk.services.mediaconvert.model.HlsGroupSettings;
import software.amazon.awssdk.services.mediaconvert.model.HlsManifestCompression;
import software.amazon.awssdk.services.mediaconvert.model.HlsManifestDurationFormat;
import software.amazon.awssdk.services.mediaconvert.model.HlsOutputSelection;
import software.amazon.awssdk.services.mediaconvert.model.HlsProgramDateTime;
import software.amazon.awssdk.services.mediaconvert.model.HlsSegmentControl;
import software.amazon.awssdk.services.mediaconvert.model.HlsStreamInfResolution;
import software.amazon.awssdk.services.mediaconvert.model.Input;
import software.amazon.awssdk.services.mediaconvert.model.JobSettings;
import software.amazon.awssdk.services.mediaconvert.model.JobStatus;
import software.amazon.awssdk.services.mediaconvert.model.Output;
import software.amazon.awssdk.services.mediaconvert.model.OutputGroup;
import software.amazon.awssdk.services.mediaconvert.model.OutputGroupSettings;
import software.amazon.awssdk.services.mediaconvert.model.OutputGroupType;
import software.amazon.awssdk.services.mediaconvert.model.VideoCodec;
import software.amazon.awssdk.services.mediaconvert.model.VideoCodecSettings;
import software.amazon.awssdk.services.mediaconvert.model.VideoDescription;

@Service
@RequiredArgsConstructor
@Slf4j
public class MediaConvertService {

    private final MediaConvertClient mediaConvertClient;
    private final MediaProperties mediaProperties;

    private static final int SEGMENT_DURATION = 6;

    /**
     * Submit HLS transcoding job for an uploaded MP4 file.
     * Output ABR ladder: 360p (800kbps), 720p (2400kbps), 1080p (4500kbps).
     * Also captures 1 JPEG thumbnail via FRAME_CAPTURE output group.
     *
     * @param media the Media entity after upload completion
     * @return the MediaConvert job ID
     */
    public String submitHlsJob(Media media) {
        MediaProperties.Aws aws = mediaProperties.getAws();
        String bucket = aws.getBucketName();
        String inputKey = media.getProviderPublicId();
        String episodeId = media.getEpisode().getEpisodeId();
        String mediaId = media.getMediaId();

        String inputS3Url = "s3://" + bucket + "/" + inputKey;
        String hlsOutputS3Url = "s3://" + bucket + "/output/videos/" + episodeId + "/" + mediaId + "/hls/";
        String thumbnailOutputS3Url = "s3://" + bucket + "/output/videos/" + episodeId + "/" + mediaId + "/thumbnails/";

        CreateJobRequest jobRequest = CreateJobRequest.builder()
                .role(aws.getMediaConvertRoleArn())
                .queue(aws.getMediaConvertQueueArn())
                .settings(buildJobSettings(inputS3Url, hlsOutputS3Url, thumbnailOutputS3Url))
                .build();

        CreateJobResponse response = mediaConvertClient.createJob(jobRequest);
        String jobId = response.job().id();

        log.info("MediaConvert job submitted. jobId={} mediaId={} input={}", jobId, mediaId, inputKey);
        return jobId;
    }

    /**
     * Poll MediaConvert job status directly. Returns true if job has finished (any terminal state).
     */
    public boolean isJobComplete(String jobId) {
        GetJobResponse response = mediaConvertClient.getJob(
                GetJobRequest.builder().id(jobId).build());
        String status = response.job().statusAsString();
        return JobStatus.COMPLETE.toString().equals(status)
                || JobStatus.ERROR.toString().equals(status)
                || JobStatus.CANCELED.toString().equals(status);
    }

    /**
     * Get job status string for reconcile polling. Returns null on error.
     */
    public String getJobStatus(String jobId) {
        try {
            GetJobResponse response = mediaConvertClient.getJob(
                    GetJobRequest.builder().id(jobId).build());
            return response.job().statusAsString();
        } catch (Exception e) {
            log.warn("Failed to get MediaConvert job status. jobId={}: {}", jobId, e.getMessage());
            return null;
        }
    }

    private JobSettings buildJobSettings(String inputUrl, String hlsOutputUrl, String thumbnailOutputUrl) {
        int[] resolutions = { 360, 720, 1080 };
        int[] bitrates = { 800_000, 2_400_000, 4_500_000 };

        Output[] hlsOutputs = new Output[resolutions.length];
        for (int i = 0; i < resolutions.length; i++) {
            hlsOutputs[i] = buildHlsRendition(resolutions[i], bitrates[i]);
        }

        OutputGroup hlsGroup = OutputGroup.builder()
                .name("Apple HLS")
                .outputGroupSettings(OutputGroupSettings.builder()
                        .type(OutputGroupType.HLS_GROUP_SETTINGS)
                        .hlsGroupSettings(buildHlsGroupSettings(hlsOutputUrl))
                        .build())
                .outputs(hlsOutputs)
                .build();

        // FRAME_CAPTURE output: captures 1 JPEG thumbnail from the first frame within the first 10 seconds
        OutputGroup thumbnailGroup = OutputGroup.builder()
                .name("Thumbnails")
                .outputGroupSettings(OutputGroupSettings.builder()
                        .type(OutputGroupType.FILE_GROUP_SETTINGS)
                        .fileGroupSettings(FileGroupSettings.builder()
                                .destination(thumbnailOutputUrl)
                                .build())
                        .build())
                .outputs(Output.builder()
                        .nameModifier("_thumb")
                        .videoDescription(VideoDescription.builder()
                                .codecSettings(VideoCodecSettings.builder()
                                        .codec(VideoCodec.FRAME_CAPTURE)
                                        .frameCaptureSettings(FrameCaptureSettings.builder()
                                                .framerateNumerator(1)
                                                .framerateDenominator(10) // 1 frame per 10 seconds
                                                .maxCaptures(1)           // only 1 thumbnail
                                                .quality(90)
                                                .build())
                                        .build())
                                .build())
                        .containerSettings(ContainerSettings.builder()
                                .container(ContainerType.RAW)
                                .build())
                        .build())
                .build();

        return JobSettings.builder()
                .inputs(Input.builder()
                        .fileInput(inputUrl)
                        .audioSelectors(java.util.Map.of(
                                "Audio Selector 1",
                                AudioSelector.builder()
                                        .defaultSelection(AudioDefaultSelection.DEFAULT)
                                        .build()))
                        .build())
                .outputGroups(hlsGroup, thumbnailGroup)
                .build();
    }

    private HlsGroupSettings buildHlsGroupSettings(String destination) {
        return HlsGroupSettings.builder()
                .destination(destination)
                .segmentLength(SEGMENT_DURATION)
                .minSegmentLength(0)
                .outputSelection(HlsOutputSelection.MANIFESTS_AND_SEGMENTS)
                .directoryStructure(HlsDirectoryStructure.SINGLE_DIRECTORY)
                .segmentControl(HlsSegmentControl.SEGMENTED_FILES)
                .manifestDurationFormat(HlsManifestDurationFormat.INTEGER)
                .programDateTime(HlsProgramDateTime.EXCLUDE)
                .captionLanguageSetting(HlsCaptionLanguageSetting.OMIT)
                .manifestCompression(HlsManifestCompression.NONE)
                .clientCache(HlsClientCache.ENABLED)
                .codecSpecification(HlsCodecSpecification.RFC_4281)
                .streamInfResolution(HlsStreamInfResolution.INCLUDE)
                .build();
    }

    private Output buildHlsRendition(int height, int bitrate) {
        String name = height + "p";
        return Output.builder()
                .nameModifier("_" + name)
                .videoDescription(VideoDescription.builder()
                        .codecSettings(VideoCodecSettings.builder()
                                .codec(VideoCodec.H_264)
                                .build())
                        .width(height * 16 / 9)
                        .height(height)
                        .build())
                .audioDescriptions(AudioDescription.builder()
                        .audioSourceName("Audio Selector 1")
                        .codecSettings(AudioCodecSettings.builder()
                                .aacSettings(AacSettings.builder()
                                        .bitrate(128_000)
                                        .codingMode(AacCodingMode.CODING_MODE_2_0)
                                        .sampleRate(48000)
                                        .build())
                                .build())
                        .build())
                .containerSettings(ContainerSettings.builder()
                        .container(ContainerType.M3_U8)
                        .build())
                .build();
    }
}
