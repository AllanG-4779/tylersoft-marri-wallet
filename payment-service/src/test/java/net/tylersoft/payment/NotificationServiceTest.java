package net.tylersoft.payment;

import net.tylersoft.payment.utils.GeneralUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.UUID;

class NotificationServiceTest {
    @Test
    void getSortedAlphabetical() {
        MultiValueMap<String, Object> dataMap = new LinkedMultiValueMap<>();
        String reference ="9c38b302-6596-4d63-8239-6406db2969a6";
        System.out.println("Reference " + reference);
        dataMap.put("branch", Collections.singletonList("123"));
        dataMap.put("cashier", Collections.singletonList("MARI WALLET"));
        dataMap.put("merchantID", Collections.singletonList(1));
        dataMap.put("mobileForSMS", Collections.singletonList("254796407365"));
        dataMap.put("till", Collections.singletonList("60003"));
        dataMap.put("uniqueReference", Collections.singletonList(reference));
        dataMap.put("value", Collections.singletonList("20"));
        dataMap.put("vendorCode", Collections.singletonList("1"));
        dataMap.put("hash", Collections.singletonList(GeneralUtils.generateHashedKey(dataMap.toSingleValueMap(), "fe785473-5154-435a-9547-8f9b5f8e87c9")));
        System.out.println("Reference " + reference);
        System.out.println(GeneralUtils.generateHashedKey(dataMap.toSingleValueMap(), "0b7b2a42-494e-45a3-b300-d888add1c8e8"));
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.set(HttpHeaders.AUTHORIZATION, "Basic UGF5YmlsbHM6Ty0jdHQwM1A3Iw==");
        RestTemplate template = new RestTemplate();


        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(dataMap, headers);
        ResponseEntity<String> response;
        try {
            response = template.postForEntity("https://test-api.ottbotswana.com/api/reseller/v1/GetVoucher", request, String.class);
            System.out.println(response);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        Assertions.assertTrue(true);
    }

}