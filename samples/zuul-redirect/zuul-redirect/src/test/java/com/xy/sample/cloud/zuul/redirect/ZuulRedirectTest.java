package com.xy.sample.cloud.zuul.redirect;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.DEFINED_PORT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Created by xiaoyao9184 on 2018/8/6.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {
        RedirectBootApplication.class
})
@SpringBootTest(webEnvironment = DEFINED_PORT)
@AutoConfigureMockMvc
public class ZuulRedirectTest {


    @Autowired
    private MockMvc mvc;

    @Test
    public void redirect_to_self_route_simple_rewrite()
            throws Exception {

        mvc.perform(get("/proxy/redirect/jump/this"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location","http://localhost/proxy/redirect/info"));
    }

    @Test
    public void redirect_to_self_route_will_rewrite()
            throws Exception {

        mvc.perform(get("/proxy/redirect/jump/self"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location","http://localhost/proxy/redirect/info"));
    }

    @Test
    public void redirect_to_other_route_will_rewrite()
            throws Exception {

        mvc.perform(get("/proxy/redirect/jump/api"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location","http://localhost/proxy/api/api-info"));
    }

    @Test
    public void redirect_to_other_route_will_passthrough()
            throws Exception {

        mvc.perform(get("/proxy/redirect/jump/bin"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location","http://httpbin.org"));
    }

    @Test
    public void redirect_to_outer_will_passthrough()
            throws Exception {

        mvc.perform(get("/proxy/redirect/jump/hub"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location","http://www.gayhub.com"));
    }

}
