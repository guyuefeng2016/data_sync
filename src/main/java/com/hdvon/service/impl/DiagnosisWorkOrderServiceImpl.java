package com.hdvon.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hdvon.entity.DiagnosisWorkOrder;
import com.hdvon.mapper.DiagnosisWorkOrderMapper;
import com.hdvon.service.IDiagnosisWorkOrderService;
import org.springframework.stereotype.Service;

@Service
public class DiagnosisWorkOrderServiceImpl
        extends ServiceImpl<DiagnosisWorkOrderMapper, DiagnosisWorkOrder> implements IDiagnosisWorkOrderService {

}
