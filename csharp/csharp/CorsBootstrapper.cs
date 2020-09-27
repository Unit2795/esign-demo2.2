using Nancy;
using Nancy.Bootstrapper;
using Nancy.TinyIoc;

public class CustomBootstrapper : DefaultNancyBootstrapper
{
    protected override void ApplicationStartup(TinyIoCContainer container, IPipelines pipelines)
    {
        pipelines.AfterRequest += ctx =>
        {
            ctx.Response.Headers.Add("Access-Control-Allow-Origin", "*");
            ctx.Response.Headers.Add("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept, Authorization");
            ctx.Response.Headers.Add("Access-Control-Allow-Methods", "DELETE");
        };
    }
}