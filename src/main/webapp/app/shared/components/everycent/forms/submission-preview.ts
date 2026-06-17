export const previewSubmittedData = <T>(source: string, values: T) => {
  window.dispatchEvent(
    new CustomEvent('everycent:form-submitted', {
      detail: {
        source,
        values,
      },
    }),
  );
};
