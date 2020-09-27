using System;
using System.Net.Http;
using System.Threading.Tasks;
using dotenv.net;
using dotenv.net.Utilities;
using Nancy;
using Flurl.Http;
using Nancy.Hosting.Self;
using Newtonsoft.Json;


namespace csharp
{
    public class Program
    {
        static async Task Main(string[] args)
        {
            using (var host = new NancyHost(new Uri("http://localhost:4567")))
            {
                host.Start();
                Console.WriteLine("Running on http://localhost:4567");
                Console.ReadLine();
            }
        }
    }
}