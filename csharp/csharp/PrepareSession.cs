using System;
using System.IO;
using System.Net.Http;
using System.Threading.Tasks;
using dotenv.net;
using dotenv.net.Utilities;
using Nancy;
using Flurl;
using Flurl.Http;
using Flurl.Http.Content;
using Nancy.Extensions;
using Nancy.Responses;
using Newtonsoft.Json;

namespace csharp
{
    public class PrepareSession : NancyModule
    {
        private static readonly HttpClient client = new HttpClient();

        static async Task<bool> validateCaptcha(string token)
        {
            var envReader = new EnvReader();
            
            var success = false;

            var responseString = await envReader.GetStringValue("CAPTCHA_ENDPOINT")
                .PostUrlEncodedAsync(new
                {
                    secret = envReader.GetStringValue("CAPTCHA_SECRET"),
                    response = token
                }).ReceiveString();
            
            dynamic responseJson = JsonConvert.DeserializeObject(responseString);

            return responseJson.success;
        }

        static async Task<string> RetrieveAccessToken()
        {
            var envReader = new EnvReader();

            var responseString = await envReader.GetStringValue("TOKEN_ENDPOINT")
                .PostUrlEncodedAsync(new
                {
                    grant_type = "client_credentials",
                    client_id = envReader.GetStringValue("ESIGN_CLIENT_ID"),
                    client_secret = envReader.GetStringValue("ESIGN_CLIENT_SECRET"),
                    scope = "read-write"
                }).ReceiveString();

            dynamic responseJson = JsonConvert.DeserializeObject(responseString);

            return "Bearer " + responseJson.access_token;
        }

        static async Task<string> RetrieveSendingSession(string token, Request request, IResponseFormatter response)
        {
            var envReader = new EnvReader();

            dynamic inc = JsonConvert.DeserializeObject(request.Form.data);

            var json = $@"
                {{
                        'folderName':'eSign Genie API Demo Documents',
                        'parties': [
                            {{
                                'firstName': '{inc.p1.fname}',
                                'lastName': '{inc.p1.lname}',
                                'emailId': '{inc.p1.email}',
                                'permission':'FILL_FIELDS_AND_SIGN',
                                'workflowSequence':1,
                                'sequence':1,
                                'allowNameChange':false
                            }},
                            {{
                                'firstName': '{inc.p2.fname}',
                                'lastName': '{inc.p2.lname}',
                                'emailId': '{inc.p2.email}',
                                'permission':'FILL_FIELDS_AND_SIGN',
                                'workflowSequence':2,
                                'sequence':2,
                                'allowNameChange':false
                            }}
                        ],
                        'processTextTags':true,
                        'processAcroFields':true,
                        'signInSequence':false,
                        'inPersonEnable':false,
                        'themeColor': '#003C1C',
                        'createEmbeddedSendingSession':true
                }}
            ";
            
            dynamic responseString;
            try
            {
                responseString = await envReader.GetStringValue("FOLDER_ENDPOINT")
                    .WithHeader("Authorization", token)
                    .PostMultipartAsync(multipart => multipart
                        .AddString("data", json)
                        .AddFile("file", request.Form.file1, "test1.pdf")
                        .AddFile("file", request.Form.file2, "test1.pdf")
                    )
                    .ReceiveString();
            }
            catch (Exception e)
            {
                Console.WriteLine(e);
                throw;
            }

            return responseString;
        }
        
        
        public PrepareSession(NancyContext context)
        {
            string path = @"E:\esign-demo2\.env";
            DotEnv.Config(true, path);

            Post("/demoform2", async _ =>
            {
                /*if (!await validateCaptcha(this.Request.Headers.Authorization))
                {
                    return new Response
                    {
                        StatusCode = HttpStatusCode.Unauthorized, ReasonPhrase = "Unauthorized"
                    };
                }*/

                return this.Request.Files.
                
                var token = await RetrieveAccessToken();
                String result = "";
                if (this.Request.Form.type == "send")
                {
                    result = await RetrieveSendingSession(token, this.Request, this.Response);
                }

                return result;
            });
        }
    }
}