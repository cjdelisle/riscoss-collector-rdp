/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.riscoss.dataproviders.providers.jira;

/**
 * @author Oscar
 */
enum IssueStatus
{

    TODO("TO DO"),
    INPROGRESS("IN PROGRESS"),
    INREVIEW("IN REVIEW"),
    OPEN("OPEN"),
    DONE("DONE"),
    CLOSED("CLOSED"),
    RESOLVED("RESOLVED"),
    UNRESOLVED("UNRESOLVED");

    private String status;

    IssueStatus(String status)
    {
        this.status = status;
    }

    public String getStatus()
    {
        return this.status;
    }
}

enum IssuePriority
{
    CRITICAL("CRITICAL"),
    BLOCKER("BLOCKER"),
    MAJOR("MAJOR"),
    MINOR("MINOR"),
    TRIVIAL("TRIVIAL");
    private String priority;

    IssuePriority(String priority)
    {
        this.priority = priority;
    }

    public String getStatus()
    {
        return this.priority;
    }
}
