# -*- coding=utf-8 -*-
from __future__ import print_function

from jira import JIRA

jira_cli = JIRA('http://jira.example.com', basic_auth=('username','password'))

issue_dict = {
    'project': {'key': 'HELPDESK'},
    'summary': 'Test Issue',
    'reporter'   : {'name':'Alice'},
    'description': 'jira-python test',
    'issuetype': {'name': 'Task'},
    'customfield_11138': {'value': 'customfield test'},
}

try:
    new_issue = jira_cli.create_issue(fields=issue_dict)
    print(new_issue)
except Exception as e:
    print(e)
