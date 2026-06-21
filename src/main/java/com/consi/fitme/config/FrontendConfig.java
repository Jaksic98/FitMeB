package com.consi.fitme.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Slf4j
@Controller
public class FrontendConfig {
  @RequestMapping("/{path:[^\\.]*}")
  public String forward(@PathVariable String path) {
    log.info("Forwarding frontend route: {}", path);
    return "forward:/index.html";
  }
}
