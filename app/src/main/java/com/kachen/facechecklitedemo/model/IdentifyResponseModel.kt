package com.kachen.facechecklitedemo.model

class IdentifyResponseModel(val NationalID: String,
                            val EnrollImage: String,
                            val Image: String,
                            val FirstName: String,
                            val LastName: String,
                            val Address: String,
                            val Score: String,
                            val EnrollDate: String): BaseResponseModel()