// Copyright 2017 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.api.graphql.grpc;

import com.google.common.base.CaseFormat;
import com.google.common.base.Converter;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Message;
import com.google.protobuf.Message.Builder;
import java.util.List;
import java.util.Map;

/**
 * Fills proto with query response data.
 *
 * 使用查询响应字段填充proto消息、proto消息可能有值。
 *
 * fixme 将map数据填充到 proto 对象中，但是解析一般是逆向的。
 */
public final class QueryResponseToProto {

  // 驼峰 -> 下划线
  private static final Converter<String, String> CAMEL_TO_UNDERSCORE = CaseFormat.LOWER_CAMEL.converterTo(CaseFormat.LOWER_UNDERSCORE);

  private QueryResponseToProto() {}

  public static <T extends Message> T buildMessage(T message, Map<String, Object> fields) {
    return (T) buildMessage(message.toBuilder(), fields);
  }

  private static Object buildMessage(Builder msgBuilder, Map<String, Object> fields) {
    /**
     * 如果字段集合为null、则返回不改变的对象
     */
    if (fields == null) {
      return msgBuilder.build();
    }

    // todo 返回消息的类型描述。
    Descriptor descriptor = msgBuilder.getDescriptorForType();

    // 遍历字段信息
    for (Map.Entry<String, Object> entry : fields.entrySet()) {

      // 如果value为空则忽略
      if (entry.getValue() == null) {
        continue;
      }

      // 获取字段描述信息
      FieldDescriptor fieldDescriptor = getFieldDescriptor(descriptor, entry.getKey());

      // 如果value是list
      if (entry.getValue() instanceof List<?>) {
        List<Object> values = (List<Object>) entry.getValue();
        // 遍历value的每一个元素，并将其
        for (Object value : values) {
          // set list元素
          msgBuilder.addRepeatedField(fieldDescriptor, buildValue(msgBuilder, fieldDescriptor, value));
        }
      } else {
        msgBuilder.setField(fieldDescriptor, buildValue(msgBuilder, fieldDescriptor, entry.getValue()));
      }
    }
    return msgBuilder.build();
  }

  private static Object buildValue(Message.Builder parentBuilder, FieldDescriptor field, Object value) {
    if (field == null) {
      return value;
    }

    if (field.getType() == FieldDescriptor.Type.MESSAGE) {
      if (field.isRepeated()) {}
      Message.Builder fieldBuilder = parentBuilder.newBuilderForField(field);
      return buildMessage(fieldBuilder, (Map<String, Object>) value);
    } else if (field.getType() == FieldDescriptor.Type.ENUM) {
      return field.getEnumType().findValueByName((String) value);
    } else {
      switch (field.getType()) {
        case FLOAT: // float is a special case
          return Float.valueOf(value.toString());
        default:
          return value;
      }
    }
  }

  /**
   * todo 非常非常有用啊！！！
   */
  private static FieldDescriptor getFieldDescriptor(Descriptor descriptor, String name) {
    // 驼峰转下划线、获取字段在proto中的定义名称
    String fieldNameInProto = CAMEL_TO_UNDERSCORE.convert(name);

    // 获取字段描述信息
    return descriptor.findFieldByName(fieldNameInProto);
  }
}
