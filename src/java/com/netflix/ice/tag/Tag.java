/*
 *
 *  Copyright 2013 Netflix, Inc.
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */
package com.netflix.ice.tag;

import java.io.Serializable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Tag implements Comparable<Tag>, Serializable {
	private static final long serialVersionUID = 1L;

	protected static Logger logger = LoggerFactory.getLogger(Tag.class);

    public static final Tag aggregated = new Tag("aggregated") {
		private static final long serialVersionUID = 1L;
		// Always put aggregated first
		@Override
        public int compareTo(Tag t) {
            return this == t ? 0 : -1;
        }
    };

    public final String name;
    Tag(String name) {
        this.name = name == null ? "" : name;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Tag)
            return this.name.equals(((Tag)o).name);
        else
            return false;
    }

    @Override
    public int hashCode() {
        return this.name.hashCode();
    }
    
    /*
     * Allow subclasses to override the name for tag sorting purposes
     */
    public String getName() {
    	return this.name;
    }

    @Override
    public String toString() {
        return getName();
    }

    public int compareTo(Tag t) {
        if (t == aggregated)
            return -t.compareTo(this);
        int result = this.getName().compareToIgnoreCase(t.getName());
        return result != 0 ? result : this.getName().compareTo(t.getName());
    }
}
