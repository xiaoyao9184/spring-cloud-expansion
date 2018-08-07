package com.xy.sample.cloud.zuul;

import com.xy.sample.cloud.zuul.ZuulApplication;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.Base64Utils;

import javax.servlet.http.Cookie;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Created by xiaoyao9184 on 2018/8/6.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {
        ZuulApplication.class
})
@SpringBootTest
@AutoConfigureMockMvc
public class ZuulBasicTest {


    @Autowired
    private MockMvc mvc;

    @Test
    public void send_info_to_basic_auth_proxy_endpoint()
            throws Exception {

        String clientIdSecret = "name:**password**";
        String base64 = Base64Utils.encodeToString(clientIdSecret.getBytes("UTF-8"));

        mvc.perform(get("/httpbin/get")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(content()
                        .contentTypeCompatibleWith(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(jsonPath("headers.Authorization").value("Basic " + base64));
    }

}
