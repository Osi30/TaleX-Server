package com.talex.server.services.recommend;

import com.talex.server.dtos.recommend.request.SeriesChannelConfigReq;
import com.talex.server.dtos.recommend.response.SeriesChannelConfigRes;
import org.apache.coyote.BadRequestException;

public interface SeriesChannelConfigService {

    SeriesChannelConfigRes getConfig();

    SeriesChannelConfigRes createConfig(SeriesChannelConfigReq req) throws BadRequestException;

    SeriesChannelConfigRes updateConfig(SeriesChannelConfigReq req) throws BadRequestException;
}