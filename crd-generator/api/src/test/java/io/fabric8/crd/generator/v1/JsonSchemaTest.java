/**
 * Copyright (C) 2015 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.crd.generator.v1;

import com.fasterxml.jackson.databind.JsonNode;
import io.fabric8.crd.example.annotated.Annotated;
import io.fabric8.crd.example.basic.Basic;
import io.fabric8.crd.example.person.Person;
import io.fabric8.crd.generator.utils.Types;
import io.fabric8.kubernetes.api.model.apiextensions.v1.JSONSchemaProps;
import io.sundr.model.TypeDef;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class JsonSchemaTest {

  @Test
  void shouldCreateJsonSchemaFromClass() {
    TypeDef person = Types.typeDefFrom(Person.class);
    JSONSchemaProps schema = JsonSchema.from(person);
    assertNotNull(schema);
    Map<String, JSONSchemaProps> properties = schema.getProperties();
    assertEquals(7, properties.size());
    final List<String> personTypes = properties.get("type").getEnum().stream().map(JsonNode::asText)
      .collect(Collectors.toList());
    assertEquals(2, personTypes.size());
    assertTrue(personTypes.contains("crazy"));
    assertTrue(personTypes.contains("crazier"));
    final Map<String, JSONSchemaProps> addressProperties = properties.get("addresses").getItems()
      .getSchema().getProperties();
    assertEquals(5, addressProperties.size());
    final List<String> addressTypes = addressProperties.get("type").getEnum().stream()
      .map(JsonNode::asText)
      .collect(Collectors.toList());
    assertEquals(2, addressTypes.size());
    assertTrue(addressTypes.contains("home"));
    assertTrue(addressTypes.contains("work"));

    final TypeDef def = Types.typeDefFrom(Basic.class);
    schema = JsonSchema.from(def);
    assertNotNull(schema);
    properties = schema.getProperties();
    assertNotNull(properties);
    assertEquals(2, properties.size());
    Map<String, JSONSchemaProps> spec = properties.get("spec").getProperties();
    assertEquals("integer", spec.get("myInt").getType());
    Map<String, JSONSchemaProps> status = properties.get("status").getProperties();
    assertEquals("string", status.get("message").getType());
  }

  @Test
  void shouldAugmentPropertiesSchemaFromAnnotations() {
    TypeDef annotated = Types.typeDefFrom(Annotated.class);
    JSONSchemaProps schema = JsonSchema.from(annotated);
    assertNotNull(schema);
    Map<String, JSONSchemaProps> properties = schema.getProperties();
    assertEquals(2, properties.size());
    final JSONSchemaProps specSchema = properties.get("spec");
    Map<String, JSONSchemaProps> spec = specSchema.getProperties();
    assertEquals(5, spec.size());

    // check descriptions are present
    assertTrue(spec.containsKey("from-field"));
    JSONSchemaProps prop = spec.get("from-field");
    assertEquals("from-field-description", prop.getDescription());
    assertTrue(spec.containsKey("from-getter"));
    prop = spec.get("from-getter");
    assertEquals("from-getter-description", prop.getDescription());

    // fields without description annotation shouldn't have them
    assertTrue(spec.containsKey("unnamed"));
    assertNull(spec.get("unnamed").getDescription());
    assertTrue(spec.containsKey("emptySetter"));
    assertNull(spec.get("emptySetter").getDescription());
    assertTrue(spec.containsKey("anEnum"));

    // check required list, should register properties with their modified name if needed
    final List<String> required = specSchema.getRequired();
    assertEquals(2, required.size());
    assertTrue(required.contains("emptySetter"));
    assertTrue(required.contains("from-getter"));

    // check the enum values
    final JSONSchemaProps anEnum = spec.get("anEnum");
    final List<JsonNode> enumValues = anEnum.getEnum();
    assertEquals(2, enumValues.size());
    enumValues.stream().map(JsonNode::textValue).forEach(s -> assertTrue("oui".equals(s) || "non".equals(s)));
  }
}
