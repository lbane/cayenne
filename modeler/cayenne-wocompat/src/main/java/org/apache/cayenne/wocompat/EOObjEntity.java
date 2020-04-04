/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.wocompat;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionException;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.map.Entity;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.map.Relationship;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.function.Function;

/**
 * An extension of ObjEntity used to accomodate extra EOModel entity properties.
 */
public class EOObjEntity extends ObjEntity {
    
	public static class UnsupportedCrossModelRelationship extends RuntimeException {
		UnsupportedCrossModelRelationship(String msg) {
			super(msg);
		}
	}
	
    protected boolean subclass;
    protected boolean abstractEntity;

    private Collection filteredQueries;
    private Map eoMap;
    private Map<String, ObjRelationship> invisibleRelationships = new HashMap<>();
	private Map<String, String> crossModelRelationship = new HashMap<>();

    public EOObjEntity() {
    }

    public EOObjEntity(String name) {
        super(name);
    }

    /**
     * Overrides super to support translation of EO attributes that have no ObjAttributes.
     * 
     * @since 1.2
     */
    @Override
    public Expression translateToDbPath(Expression expression) {

        if (expression == null) {
            return null;
        }

        if (getDbEntity() == null) {
            throw new CayenneRuntimeException(
                    "Can't translate expression to DB_PATH, no DbEntity for '"
                            + getName()
                            + "'.");
        }

        // converts all OBJ_PATH expressions to DB_PATH expressions
        // and pass control to the DB entity
        return expression.transform(new DBPathConverter());
    }

    /**
     * @since 1.2
     */
    // TODO: andrus, 5/27/2006 - make public after 1.2. Also maybe move entity
    // initialization code from EOModelProcessor to this class, kind of like EOQuery does.
    Map getEoMap() {
        return eoMap;
    }

    /**
     * @since 1.2
     */
    // TODO: andrus, 5/27/2006 - make public after 1.2. Also maybe move entity
    // initialization code from EOModelProcessor to this class, kind of like EOQuery does.
    void setEoMap(Map eoMap) {
        this.eoMap = eoMap;
    }

    /**
     * Returns a collection of queries for this entity.
     * 
     * @since 1.1
     */
    public Collection getEOQueries() {
        if (filteredQueries == null) {
            Collection queries = getDataMap().getQueryDescriptors();
            if (queries.isEmpty()) {
                filteredQueries = Collections.EMPTY_LIST;
            }
            else {
                Map params = Collections.singletonMap("root", EOObjEntity.this);
                Expression filter = ExpressionFactory.exp("root = $root").params(params);
                filteredQueries = filter.filter(queries, new ArrayList());
            }
        }

        return filteredQueries;
    }

    public boolean isAbstractEntity() {
        return abstractEntity;
    }

    public void setAbstractEntity(boolean abstractEntity) {
        this.abstractEntity = abstractEntity;
    }

    public boolean isSubclass() {
        return subclass;
    }

    public void setSubclass(boolean subclass) {
        this.subclass = subclass;
    }

	public void addInvisibleRelationship(ObjRelationship rel) {
		invisibleRelationships.put(rel.getName(), rel);
	}
	
	public ObjRelationship getInvisibleRelationship(String relName) {
		return invisibleRelationships.get(relName);
	}
	
	public void addCrossModelRelationship(String relName, String targetName) {
		crossModelRelationship.put(relName, targetName);
	}
	
	public String getCrossModelRelationshipTarget(String relName) {
		return crossModelRelationship.get(relName);
	}
	
    /**
     * Translates query name local to the ObjEntity to the global name. This translation
     * is needed since EOModels store queries by entity, while Cayenne DataMaps store them
     * globally.
     * 
     * @since 1.1
     */
    public String qualifiedQueryName(String queryName) {
        return getName() + "_" + queryName;
    }

    /**
     * @since 1.1
     */
    public String localQueryName(String qualifiedQueryName) {
        return (qualifiedQueryName != null && qualifiedQueryName.startsWith(getName()
                + "_"))
                ? qualifiedQueryName.substring(getName().length() + 1)
                : qualifiedQueryName;
    }

    final class DBPathConverter implements Function<Object, Object> {

        public Object apply(Object input) {

            if (!(input instanceof Expression)) {
                return input;
            }

            Expression expression = (Expression) input;

            if (expression.getType() != Expression.OBJ_PATH) {
                return input;
            }

            // convert obj_path to db_path

            StringBuilder buffer = new StringBuilder();
            EOObjEntity entity = EOObjEntity.this;
            StringTokenizer toks = new StringTokenizer(expression.toString(), ".");
            while (toks.hasMoreTokens() && entity != null) {
                String chunk = toks.nextToken();

                if (toks.hasMoreTokens()) {
                    // this is a relationship
                    if (buffer.length() > 0) {
                        buffer.append(Entity.PATH_SEPARATOR);
                    }
                    
                    // we build a db path, so will also check dbEntity relationship
                    Relationship r = entity.getDbEntity().getRelationship(chunk);
                    if (r == null) {
                    	
                    	String target = entity.getCrossModelRelationshipTarget(chunk);
                    	if (target != null) {
                    		throw new UnsupportedCrossModelRelationship("Cross model relationship '"+chunk+"' from '"+entity.getName()+"' to '"+target+"' not supported");
                    	}
                    	
                    	throw new ExpressionException("Invalid path component: " + chunk);
                    }
                    
                    buffer.append(chunk);

                    // but the EO-Relationships are build with Entity-relationships, and we need to get
                    r = entity.getRelationship(chunk);
                    if (r == null) {
                    	
                    	// some of the eoenity relationships are not entity attributes (visible)
                    	// check those also
                    	r = entity.getInvisibleRelationship(chunk);
                    	
                    	if (r == null) {
                    		throw new ExpressionException("Invalid path component: " + chunk);
                    	}
                    }
                    

                    entity = (EOObjEntity) r.getTargetEntity();
                }
                else {
                	// this is an attribute OR a relationship

                    Relationship r = entity.getDbEntity().getRelationship(chunk);
                    if (r != null) {
                        if (buffer.length() > 0) {
                            buffer.append(Entity.PATH_SEPARATOR);
                        }
                    	buffer.append(chunk);
                    }
                    else {
                    	List attributes = (List) entity.getEoMap().get("attributes");
                    	Iterator it = attributes.iterator();
                    	while (it.hasNext()) {
                    		Map attribute = (Map) it.next();
                    		if (chunk.equals(attribute.get("name"))) {

                    			if (buffer.length() > 0) {
                    				buffer.append(Entity.PATH_SEPARATOR);
                    			}

                    			buffer.append(attribute.get("columnName"));
                    			break;
                    		}
                    	}
                    }
                }
            }

            Expression exp = ExpressionFactory.expressionOfType(Expression.DB_PATH);
            exp.setOperand(0, buffer.toString());
            return exp;
        }
    }
}
