package com.easypan.rag.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.easypan.entity.po.DocChunk;
import com.easypan.mappers.DocChunkMapper;
import com.easypan.rag.service.DocChunkService;
import org.springframework.stereotype.Service;

@Service
public class DocChunkServiceImpl extends ServiceImpl<DocChunkMapper, DocChunk> implements DocChunkService {
}
