/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2009 by Red Hat Inc and/or its affiliates or by
 * third-party contributors as indicated by either @author tags or express
 * copyright attribution statements applied by the authors.  All
 * third-party contributions are distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.jpa.metamodel.internal;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.ManagedType;

import org.hibernate.internal.util.ReflectHelper;

/**
 * Models the commonality of the JPA {@link Attribute} hierarchy.
 *
 * @author Steve Ebersole
 */
public abstract class AbstractAttribute<X, Y> implements Attribute<X, Y>, Serializable {
	private final String name;
	private final Class<Y> javaType;
	private final AbstractManagedType<X> declaringType;
	private transient Member member;
	private final PersistentAttributeType persistentAttributeType;

	public AbstractAttribute(
			String name,
			Class<Y> javaType,
			AbstractManagedType<X> declaringType,
			Member member,
			PersistentAttributeType persistentAttributeType) {
		this.name = name;
		this.javaType = javaType;
		this.declaringType = declaringType;
		this.member = member;
		this.persistentAttributeType = persistentAttributeType;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public ManagedType<X> getDeclaringType() {
		return declaringType;
	}

	@Override
	public Class<Y> getJavaType() {
		return javaType;
	}

	@Override
	public Member getJavaMember() {
		return member;
	}

	@Override
	public PersistentAttributeType getPersistentAttributeType() {
		return persistentAttributeType;
	}

	/**
	 * Used by JDK serialization...
	 *
	 * @param oos The output stream to which we are being written...
	 * @throws IOException Indicates a general IO stream exception
	 */
	protected void writeObject(ObjectOutputStream oos) throws IOException {
		oos.defaultWriteObject();
		oos.writeObject( getJavaMember().getDeclaringClass().getName() );
		oos.writeObject( getJavaMember().getName() );
		// should only ever be a field or the getter-method...
		oos.writeObject( Method.class.isInstance( getJavaMember() ) ? "method" : "field" );
	}

	/**
	 * Used by JDK serialization...
	 *
	 * @param ois The input stream from which we are being read...
	 * @throws java.io.IOException Indicates a general IO stream exception
	 * @throws ClassNotFoundException Indicates a class resolution issue
	 */
	protected void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		ois.defaultReadObject();
		final String memberDeclaringClassName = ( String ) ois.readObject();
		final String memberName = ( String ) ois.readObject();
		final String memberType = ( String ) ois.readObject();

		final Class memberDeclaringClass = Class.forName(
				memberDeclaringClassName,
				false,
				declaringType.getJavaType().getClassLoader()
		);
		try {
			this.member = "method".equals( memberType )
					? memberDeclaringClass.getMethod( memberName, ReflectHelper.NO_PARAM_SIGNATURE )
					: memberDeclaringClass.getField( memberName );
		}
		catch ( Exception e ) {
			throw new IllegalStateException(
					"Unable to locate member [" + memberDeclaringClassName + "#"
							+ memberName + "]"
			);
		}
	}
}