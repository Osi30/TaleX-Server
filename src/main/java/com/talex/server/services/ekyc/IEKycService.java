package com.talex.server.services.ekyc;

import com.talex.server.dtos.responses.idrecognition.back.FptAiIdBackResponse;
import com.talex.server.dtos.responses.idrecognition.front.FptAiIdFrontResponse;
import com.talex.server.dtos.responses.liveness.FptAiLivenessResponse;
import org.springframework.web.multipart.MultipartFile;

public interface IEKycService {
    FptAiIdFrontResponse processFrontSide(MultipartFile file);

    FptAiIdBackResponse processBackSide(MultipartFile file);

    FptAiLivenessResponse checkLiveness(MultipartFile videoFile, MultipartFile cmndFile);
}
