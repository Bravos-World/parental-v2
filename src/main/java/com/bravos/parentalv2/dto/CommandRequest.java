package com.bravos.parentalv2.dto;

import com.bravos.parentalv2.model.CommandType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CommandRequest {

  @NotNull(message = "Command type is required")
  private CommandType commandType;

  private int delaySeconds = 60;

}
