/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * $Id: HexBinaryDatatypeValidator.hpp 695949 2008-09-16 15:57:44Z borisk $
 */

#if !defined(XERCESC_INCLUDE_GUARD_HEXBINARY_DATATYPEVALIDATOR_HPP)
#define XERCESC_INCLUDE_GUARD_HEXBINARY_DATATYPEVALIDATOR_HPP

#include "xercesc/validators/datatype/AbstractStringValidator.hpp"

XERCES_CPP_NAMESPACE_BEGIN

class VALIDATORS_EXPORT HexBinaryDatatypeValidator : public AbstractStringValidator
{
public:

    // -----------------------------------------------------------------------
    //  Public ctor/dtor
    // -----------------------------------------------------------------------
	/** @name Constructors and Destructor */
    //@{

    HexBinaryDatatypeValidator
    (
        MemoryManager* const manager = XMLPlatformUtils::fgMemoryManager
    );
    HexBinaryDatatypeValidator
    (
        DatatypeValidator* const baseValidator
        , RefHashTableOf<KVStringPair>* const facets
        , RefArrayVectorOf<XMLCh>* const enums
        , const int finalSet
        , MemoryManager* const manager = XMLPlatformUtils::fgMemoryManager
    );

    virtual ~HexBinaryDatatypeValidator();

	//@}

    /**
      * Returns an instance of the base datatype validator class
	  * Used by the DatatypeValidatorFactory.
      */
    virtual DatatypeValidator* newInstance
    (
        RefHashTableOf<KVStringPair>* const facets
        , RefArrayVectorOf<XMLCh>* const enums
        , const int finalSet
        , MemoryManager* const manager = XMLPlatformUtils::fgMemoryManager
    );

    /***
     * Support for Serialization/De-serialization
     ***/
    DECL_XSERIALIZABLE(HexBinaryDatatypeValidator)

protected:

    virtual void checkValueSpace(const XMLCh* const content
                                , MemoryManager* const manager);

    virtual XMLSize_t  getLength(const XMLCh* const content
                       , MemoryManager* const manager) const;

private:
    // -----------------------------------------------------------------------
    //  Unimplemented constructors and operators
    // -----------------------------------------------------------------------
    HexBinaryDatatypeValidator(const HexBinaryDatatypeValidator&);
    HexBinaryDatatypeValidator& operator=(const HexBinaryDatatypeValidator&);

    // -----------------------------------------------------------------------
    //  Private data members
    //
	//		Nil.
    // -----------------------------------------------------------------------
};

XERCES_CPP_NAMESPACE_END

#endif

/**
  * End of file HexBinaryDatatypeValidator.hpp
  */
