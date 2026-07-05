package com.consi.fitme.exception.termintemplate;

import com.consi.fitme.exception.base.CustomException;
import com.consi.fitme.model.ErrorCode;

public class TerminTemplateOverlapException extends CustomException {
  public TerminTemplateOverlapException() {
    super(
        "Šablon termina se preklapa sa postojećim šablonom istog dana u nedelji",
        ErrorCode.TERMIN_TEMPLATE_OVERLAP);
  }
}
