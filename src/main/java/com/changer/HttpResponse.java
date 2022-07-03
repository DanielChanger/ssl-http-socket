/*
Copyright 2022-present © Care.com, Inc. All rights reserved.
This software is the confidential and proprietary information of Care.com, Inc.
*/
package com.changer;


import java.util.List;

public record HttpResponse(List<String> headers, List<String> body) {}
