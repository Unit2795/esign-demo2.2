'use strict';
const fetch = require("node-fetch");
const url = require('url');
const GoogleRecaptcha = require('google-recaptcha')
const AWS = require('aws-sdk');
const querystring = require('querystring');
const { v4: uuidv4 } = require('uuid');
const s3 = new AWS.S3({
  region: 'us-east-1',
  signatureVersion: 'v4',
})

const googleRecaptcha = new GoogleRecaptcha({secret: process.env.CAPTCHA_SECRET})

const corsHeaders = {
  "Access-Control-Allow-Origin" : "*",
  "Access-Control-Allow-Credentials": true
}

async function validateCaptcha(token)
{
  const params = new url.URLSearchParams();
  params.append('secret', process.env.CAPTCHA_SECRET);
  params.append('response', token);

  let options = {
    "method": "post",
    "body": params
  };

  let tokenResult;
  try
  {
    tokenResult = await fetch(process.env.CAPTCHA_ENDPOINT, options);
  }
  catch (e) {
    console.log("Error: " + JSON.stringify(e))
  }

  let data = await tokenResult.json();

  return data.success;
}

async function retrieveAccessToken()
{
  if (process.env.ESIGN_ACCESS_TOKEN)
  {
    return "Bearer " + process.env.ESIGN_ACCESS_TOKEN;
  }
  const params = new url.URLSearchParams();
  params.append('grant_type', 'client_credentials');
  params.append('client_id', process.env.ESIGN_CLIENT_ID);
  params.append('client_secret', process.env.ESIGN_CLIENT_SECRET);
  params.append('scope', 'read-write');

  let tokenOptions =
    {
      "method": "post",
      "headers": {
        "Origin": process.env.CORS_ORIGIN
      },
      "body": params
    };

  let tokenResult = await fetch(process.env.TOKEN_ENDPOINT, tokenOptions);

  let data = await tokenResult.json();

  process.env.ESIGN_ACCESS_TOKEN = data.access_token;
  return "Bearer " + data.access_token;
}


module.exports.createfolder = async event => {
  let body = JSON.stringify(JSON.parse(event.body));
  let result;
  try
  {
    let options = {
      method: "post",
      headers: {
        "Authorization": await retrieveAccessToken(),
        "Content-Type": "application/json"
      },
      body
    }

    result = await fetch(process.env.FOLDER_ENDPOINT, options)
  } catch (e) {
    console.log(e);
  }

  let stringBody = await result.text();
  console.log(stringBody);

  return {
    statusCode: 200,
    headers: corsHeaders,
    body: stringBody
  };
};

module.exports.senddraft = async event => {
  let result = await fetch(process.env.SEND_DRAFT_ENDPOINT, {
    method: "post",
    headers: {
      "Authorization": await retrieveAccessToken(),
      "Content-Type": "application/json"
    },
    body: JSON.stringify({
      folderId: event.body
    })
  });


  return {
    statusCode: 200,
    headers: corsHeaders,
    body: await result.text()
  }
};

module.exports.presign = async event => {
  /*if (!await validateCaptcha(event.headers.Authorization)) {
    return {
      statusCode: 401,
      headers: corsHeaders,
      body: "Unauthorized"
    }
  }*/


  let incoming = querystring.parse(event.body);
  let fileCount = incoming.fileCount;

  if (fileCount > 3)
  {
    return {
      statusCode: 400,
      headers: corsHeaders,
      body: "Over 3 files"
    }
  }

  let urls = [];
  let files = [];

  for (let i = 0; i < fileCount; i++)
  {
    const fileKey = uuidv4() + ".pdf";

    const presignedURL = s3.getSignedUrl('putObject', {
      Bucket: "esign2-demo-tempfiles",
      Key: fileKey,
      Expires: 300
    });

    urls.push(presignedURL);
    files.push(fileKey);
  }

  return {
    statusCode: 200,
    headers: corsHeaders,
    body: JSON.stringify({
      urls,
      files
    })
  };
};
