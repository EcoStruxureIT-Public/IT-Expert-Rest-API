package com.ecostruxureit.api.sample.webui;

import com.ecostruxureit.api.sample.AlarmRepository;
import java.util.Objects;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Copyright © 2025 Schneider Electric. All Rights Reserved.
 * <p>
 * Handles the page that renders all alarms stored in the database.
 */
@Controller
public class AlarmController {

    public static final String ALARMS_PATH = "/alarms";

    private final AlarmRepository alarmRepository;

    public AlarmController(AlarmRepository alarmRepository) {
        this.alarmRepository = Objects.requireNonNull(alarmRepository);
    }

    @GetMapping(ALARMS_PATH)
    public String alarms(Model model) {

        model.addAttribute("alarms", alarmRepository.findAll());

        // Rendered by the template by the same name + html suffix (the model is made available to the template)
        return "alarm-list";
    }
}
