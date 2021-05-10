package com.baidu.hugegraph.computer.core.store.iter;

import java.io.Closeable;
import java.util.Iterator;

import com.baidu.hugegraph.computer.core.store.entry.KvEntry;

public interface InputIterator extends Iterator<KvEntry>, Closeable {
}
