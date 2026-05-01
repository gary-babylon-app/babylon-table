/*
 * Copyright 2026 Babylon Financial Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */

package app.babylon.table.transform.dsl;

import app.babylon.table.transform.Transform;

@FunctionalInterface
public interface TransformCommandParser
{
    Transform parse(TokenStream tokens);
}
