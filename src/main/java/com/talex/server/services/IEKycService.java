package com.talex.server.services;

import com.talex.server.dtos.responses.idrecognition.back.FptAiIdBackResponse;
import com.talex.server.dtos.responses.idrecognition.front.FptAiIdFrontResponse;
import org.springframework.web.multipart.MultipartFile;

public interface IEKycService {
    FptAiIdFrontResponse processFrontSide(MultipartFile file);

    FptAiIdBackResponse processBackSide(MultipartFile file);
}
