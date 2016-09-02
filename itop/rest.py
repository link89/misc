# -*- coding=utf-8 -*-
import request
import json

itop_url = 'http://127.0.0.1/web/webservices/rest.php'

req = {
    'version' : '1.0',
    'json_data' : json.dumps({
        'operation'     : 'core/get',
        'class'         : 'Person',
        'key'           : "SELECT Person WHERE Person.name = 'Henry'",
        'output_fields' : 'ext_id, first_name, name, employee_number, email, mobile_phone, org_id->id, manager_id->id',
    })
}
res = request('POST', itop_url, data = req)

req = {
    'version' : '1.0',
    'json_data' : json.dumps({
        'operation'     : 'core/update',
        'comment'       : 'update by rest',
        'class'         : 'Person',
        'key'           : {'name' : 'Henry'},
        'output_fields' : 'id',
        'fields'        : {'employee_number' : '12345678',
                           'org_id'          : "SELECT Organization FROM Organization WHERE Organization.name = 'Development'",
                           'manager_id'      : "SELECT Person FROM Person WHERE Person.name = 'Bob'"
                           }
    })
}
res = request('POST', itop_url, data = req)

# complex one
req = {
    'version' : '1.0',
    'json_data' : json.dumps({
        'operation'     : 'core/update',
        'comment'       : 'update by rest',
        'class'         : 'UserExternal',
        'key'           : "SELECT UserExternal FROM UserExternal JOIN Person ON UserExternal.contactid=Person.id WHERE Person.name='Bob'",
        'output_fields' : 'id',
        'fields'        : {'profile_list': [{u'profileid': u'2'}]}
    })
}
res = request('POST', itop_url, data = req)
