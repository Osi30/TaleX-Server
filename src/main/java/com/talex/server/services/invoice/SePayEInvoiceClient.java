package com.talex.server.services.invoice;

import com.talex.server.dtos.requests.invoice.SePayCreateInvoiceRequestDto;
import com.talex.server.dtos.responses.invoice.SePayCreateInvoiceResponseDataDto;
import com.talex.server.dtos.responses.invoice.SePayInvoiceStatusDataDto;
import com.talex.server.dtos.responses.invoice.SePayProviderAccountDetailDto;

public interface SePayEInvoiceClient {
    SePayProviderAccountDetailDto resolveDefaultProviderAccount();

    SePayCreateInvoiceResponseDataDto createInvoice(SePayCreateInvoiceRequestDto request);

    SePayInvoiceStatusDataDto checkCreationStatus(String trackingCode);
}
